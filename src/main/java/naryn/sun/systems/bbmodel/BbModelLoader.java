package naryn.sun.systems.bbmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Загрузчик .bbmodel (родной формат проекта Blockbench, НЕ экспортированный
 * "Bedrock geometry" — сохранённый .bbmodel-файл можно скармливать сюда напрямую,
 * без дополнительного экспорта).
 *
 * ЧТО ПОДДЕРЖАНО (минимальный набор, достаточный для типичной модели типа дракона):
 *  - элементы типа "cube" (from/to/origin/rotation/faces с UV)
 *  - дерево костей outliner (группы с origin/rotation, вложенность)
 *  - одна встроенная (base64) текстура — textures[0].source
 *  - анимации: каналы position/rotation/scale, линейная интерполяция
 *
 * ЧТО НЕ ПОДДЕРЖАНО (осознанно, чтобы не тащить весь Blockbench):
 *  - элементы типа "mesh" / "locator" / "null_object" — тихо пропускаются
 *  - несколько текстур на одну модель — берётся только textures[0]
 *  - текстуры по внешнему пути на диске (source не в виде data:base64) — пропускаются
 *  - Molang-выражения в keyframe data_points (например "query.anim_time*2") — если
 *    значение не парсится как обычное число, считается 0. Для взмахов/вращений,
 *    заданных руками в Blockbench (а не формулой), это не проблема.
 *  - интерполяция Catmull-Rom/Step — обе трактуются как линейная (см. BbChannelTrack)
 */
public final class BbModelLoader {

    private BbModelLoader() {
    }

    /**
     * Загружает модель из ресурса мода (assets/{namespace}/bbmodels/{path}.bbmodel и т.п.).
     * modelKey используется только как человекочитаемый идентификатор текстуры/логов.
     */
    public static BbModel loadFromResource(Identifier resourceId) throws IOException {
        var resourceOpt = MinecraftClient.getInstance().getResourceManager().getResource(resourceId);
        if (resourceOpt.isEmpty()) {
            throw new IOException("bbmodel resource not found: " + resourceId);
        }
        String modelKey = resourceId.getPath().replace('/', '_').replace(".bbmodel", "");
        try (InputStream is = resourceOpt.get().getInputStream()) {
            return load(resourceId.getNamespace(), modelKey, is);
        }
    }

    public static BbModel load(String textureNamespace, String modelKey, InputStream jsonStream) throws IOException {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(jsonStream, StandardCharsets.UTF_8)).getAsJsonObject();

        int texW = 16, texH = 16;
        if (root.has("resolution")) {
            JsonObject res = root.getAsJsonObject("resolution");
            texW = res.has("width") ? res.get("width").getAsInt() : 16;
            texH = res.has("height") ? res.get("height").getAsInt() : 16;
        }

        Identifier textureId = null;
        NativeImage image = decodeFirstTexture(root);
        if (image != null) {
            textureId = Identifier.of(textureNamespace, "bbmodel/" + modelKey.toLowerCase(java.util.Locale.ROOT));
            NativeImageBackedTexture texture =       new NativeImageBackedTexture(image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
        }

                Map<String, BbCube> cubesByUuid = root.has("elements")
            ? parseElements(root.getAsJsonArray("elements"))
            : new HashMap<>();

        // В современном .bbmodel name/origin/rotation каждой кости лежат не на узле outliner,
        // а в отдельном top-level массиве "groups" (по тому же uuid) — outliner там только
        // задаёт структуру дерева.
        Map<String, JsonObject> groupMetaByUuid = new HashMap<>();
        if (root.has("groups")) {
            for (JsonElement el : root.getAsJsonArray("groups")) {
                JsonObject g = el.getAsJsonObject();
                if (g.has("uuid")) {
                    groupMetaByUuid.put(g.get("uuid").getAsString(), g);
                }
            }
        }

        Map<String, String> boneNameByUuid = new HashMap<>();
        List<BbBone> roots = root.has("outliner")
            ? parseOutliner(root.getAsJsonArray("outliner"), cubesByUuid, groupMetaByUuid, boneNameByUuid)
            : new ArrayList<>();

        BbModel model = new BbModel(modelKey, texW, texH, textureId);
        model.rootBones.addAll(roots);
        model.animations.putAll(parseAnimations(root, boneNameByUuid));
        return model;
    }

    // ---------------------------------------------------------------- elements/cubes

    private static Map<String, BbCube> parseElements(JsonArray elements) {
        Map<String, BbCube> map = new HashMap<>();
        for (JsonElement el : elements) {
            JsonObject o = el.getAsJsonObject();
            String type = o.has("type") ? o.get("type").getAsString() : "cube";
            if (!"cube".equals(type)) {
                continue; // mesh/locator/null_object — не поддержаны, см. класс-докстринг
            }
            if (!o.has("uuid")) continue;
            String uuid = o.get("uuid").getAsString();

            Vector3f from = readVec3(o.get("from"), 0, 0, 0);
            Vector3f to = readVec3(o.get("to"), 0, 0, 0);
            Vector3f origin = o.has("origin") ? readVec3(o.get("origin"), 0, 0, 0) : null;
            Vector3f rotation = o.has("rotation") ? readVec3(o.get("rotation"), 0, 0, 0) : null;

            Map<BbCube.Face, float[]> faces = new EnumMap<>(BbCube.Face.class);
            if (o.has("faces")) {
                JsonObject facesObj = o.getAsJsonObject("faces");
                putFace(faces, facesObj, "north", BbCube.Face.NORTH);
                putFace(faces, facesObj, "south", BbCube.Face.SOUTH);
                putFace(faces, facesObj, "east", BbCube.Face.EAST);
                putFace(faces, facesObj, "west", BbCube.Face.WEST);
                putFace(faces, facesObj, "up", BbCube.Face.UP);
                putFace(faces, facesObj, "down", BbCube.Face.DOWN);
            }

            map.put(uuid, new BbCube(from, to, origin, rotation, faces));
        }
        return map;
    }

    private static void putFace(Map<BbCube.Face, float[]> out, JsonObject facesObj, String key, BbCube.Face face) {
        if (!facesObj.has(key)) return;
        JsonObject f = facesObj.getAsJsonObject(key);
        if (!f.has("uv")) return;
        JsonArray uv = f.getAsJsonArray("uv");
        if (uv.size() < 4) return;
        out.put(face, new float[]{
            uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
            uv.get(2).getAsFloat(), uv.get(3).getAsFloat()
        });
    }

    // ---------------------------------------------------------------- outliner/bones

        private static List<BbBone> parseOutliner(JsonArray outliner, Map<String, BbCube> cubesByUuid,
                                               Map<String, JsonObject> groupMetaByUuid,
                                               Map<String, String> boneNameByUuidOut) {
        List<BbBone> roots = new ArrayList<>();
        BbBone implicitRoot = null; // для кубов, лежащих в outliner без обёртки в группу
        for (JsonElement el : outliner) {
            if (el.isJsonPrimitive()) {
                BbCube cube = cubesByUuid.get(el.getAsString());
                if (cube == null) continue;
                if (implicitRoot == null) {
                    implicitRoot = new BbBone("root", new Vector3f(0, 0, 0), new Vector3f(0, 0, 0));
                    roots.add(implicitRoot);
                }
                implicitRoot.cubes.add(cube);
            } else if (el.isJsonObject()) {
                roots.add(parseBoneGroup(el.getAsJsonObject(), cubesByUuid, groupMetaByUuid, boneNameByUuidOut));
            }
        }
        return roots;
    }

        private static BbBone parseBoneGroup(JsonObject group, Map<String, BbCube> cubesByUuid,
                                          Map<String, JsonObject> groupMetaByUuid,
                                          Map<String, String> boneNameByUuidOut) {
        String uuid = group.has("uuid") ? group.get("uuid").getAsString() : null;
        // Реальные name/origin/rotation в этом файле лежат в top-level "groups" по uuid,
        // не на самом узле outliner — см. правку в load(). Если по какой-то причине метаданных
        // для этого uuid нет (старый/другой формат .bbmodel), откатываемся на сам узел outliner,
        // как раньше.
        JsonObject meta = uuid != null ? groupMetaByUuid.get(uuid) : null;
        JsonObject source = meta != null ? meta : group;

        String name = source.has("name") ? source.get("name").getAsString() : (uuid != null ? uuid : "bone");
        Vector3f origin = source.has("origin") ? readVec3(source.get("origin"), 0, 0, 0) : new Vector3f(0, 0, 0);
        Vector3f rotation = source.has("rotation") ? readVec3(source.get("rotation"), 0, 0, 0) : new Vector3f(0, 0, 0);
        BbBone bone = new BbBone(name, origin, rotation);

        if (uuid != null) {
            boneNameByUuidOut.put(uuid, name);
        }

        if (group.has("children")) {
            for (JsonElement childEl : group.getAsJsonArray("children")) {
                if (childEl.isJsonPrimitive()) {
                    BbCube cube = cubesByUuid.get(childEl.getAsString());
                    if (cube != null) bone.cubes.add(cube);
                } else if (childEl.isJsonObject()) {
                    bone.children.add(parseBoneGroup(childEl.getAsJsonObject(), cubesByUuid, groupMetaByUuid, boneNameByUuidOut));
                }
            }
        }
        return bone;
    }

    // ---------------------------------------------------------------- animations

    private static Map<String, BbAnimation> parseAnimations(JsonObject root, Map<String, String> boneNameByUuid) {
        Map<String, BbAnimation> result = new HashMap<>();
        if (!root.has("animations")) return result;

        for (JsonElement animEl : root.getAsJsonArray("animations")) {
            JsonObject animObj = animEl.getAsJsonObject();
            String name = animObj.has("name") ? animObj.get("name").getAsString() : "animation";
            float length = animObj.has("length") ? animObj.get("length").getAsFloat() : 1.0F;
            boolean loop = animObj.has("loop") && "loop".equals(animObj.get("loop").getAsString());
            BbAnimation anim = new BbAnimation(name, length, loop);

            if (animObj.has("animators")) {
                JsonObject animators = animObj.getAsJsonObject("animators");
                for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                    String boneName = boneNameByUuid.get(entry.getKey());
                    if (boneName == null) continue; // трек на кость, которой нет в outliner — пропускаем

                    JsonObject animatorObj = entry.getValue().getAsJsonObject();
                    BbBoneAnimator animator = new BbBoneAnimator();
                    if (animatorObj.has("keyframes")) {
                        for (JsonElement kfEl : animatorObj.getAsJsonArray("keyframes")) {
                            JsonObject kf = kfEl.getAsJsonObject();
                            String channel = kf.has("channel") ? kf.get("channel").getAsString() : "rotation";
                            float time = kf.has("time") ? kf.get("time").getAsFloat() : 0F;
                            Vector3f value = readDataPoint(kf);
                            BbChannelTrack track = switch (channel) {
                                case "position" -> animator.position;
                                case "scale" -> animator.scale;
                                default -> animator.rotation;
                            };
                            track.add(time, value);
                        }
                    }
                    animator.position.sortByTime();
                    animator.rotation.sortByTime();
                    animator.scale.sortByTime();
                    anim.animatorsByBoneName.put(boneName, animator);
                }
            }
            result.put(name, anim);
        }
        return result;
    }

    private static Vector3f readDataPoint(JsonObject keyframe) {
        if (!keyframe.has("data_points")) return new Vector3f(0, 0, 0);
        JsonArray dp = keyframe.getAsJsonArray("data_points");
        if (dp.isEmpty()) return new Vector3f(0, 0, 0);
        JsonObject p = dp.get(0).getAsJsonObject();
        return new Vector3f(parseFloatSafe(p, "x"), parseFloatSafe(p, "y"), parseFloatSafe(p, "z"));
    }

    private static float parseFloatSafe(JsonObject p, String key) {
        if (!p.has(key)) return 0F;
        try {
            return Float.parseFloat(p.get(key).getAsString());
        } catch (NumberFormatException e) {
            return 0F; // Molang-формула вместо числа — не поддержана, см. класс-докстринг
        }
    }

    // ---------------------------------------------------------------- texture/misc

    private static NativeImage decodeFirstTexture(JsonObject root) throws IOException {
        if (!root.has("textures")) return null;
        JsonArray textures = root.getAsJsonArray("textures");
        if (textures.isEmpty()) return null;
        JsonObject tex = textures.get(0).getAsJsonObject();
        if (!tex.has("source")) return null;
        String source = tex.get("source").getAsString();
        int comma = source.indexOf(',');
        if (!source.startsWith("data:") || comma < 0) return null; // внешний путь на диске — не поддержан
        byte[] bytes = Base64.getDecoder().decode(source.substring(comma + 1));
        return NativeImage.read(bytes);
    }

    private static Vector3f readVec3(JsonElement el, float dx, float dy, float dz) {
        if (el == null || !el.isJsonArray()) return new Vector3f(dx, dy, dz);
        JsonArray a = el.getAsJsonArray();
        float x = a.size() > 0 ? a.get(0).getAsFloat() : dx;
        float y = a.size() > 1 ? a.get(1).getAsFloat() : dy;
        float z = a.size() > 2 ? a.get(2).getAsFloat() : dz;
        return new Vector3f(x, y, z);
    }
}
