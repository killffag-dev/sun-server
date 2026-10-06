package naryn.sun.systems.modules.modules.optimization;

import lombok.Generated;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.SelectSetting;

@ModuleInfo(name = "NoRender", category = ModuleCategory.OPTIMIZATION, enabledByDefault = false)
public class NoRender extends BaseModule {

    // Сущности
    private final SelectSetting entities = new SelectSetting(this, "modules.settings.norender.entities");
    private final SelectSetting.Value players = new SelectSetting.Value(this.entities, "modules.settings.norender.entities.players");
    private final SelectSetting.Value mobs = new SelectSetting.Value(this.entities, "modules.settings.norender.entities.mobs");
    private final SelectSetting.Value items = new SelectSetting.Value(this.entities, "modules.settings.norender.entities.items");
    private final SelectSetting.Value armorStands = new SelectSetting.Value(this.entities, "modules.settings.norender.entities.armor_stands");
    private final SelectSetting.Value fallingBlocks = new SelectSetting.Value(this.entities, "modules.settings.norender.entities.falling_blocks");
    private final SelectSetting.Value tnt = new SelectSetting.Value(this.entities, "modules.settings.norender.entities.tnt");

    // Блочные сущности (Tile Entities)
    private final SelectSetting tileEntities = new SelectSetting(this, "modules.settings.norender.tile_entities");
    private final SelectSetting.Value chests = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.chests");
    private final SelectSetting.Value enderChests = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.ender_chests");
    private final SelectSetting.Value shulkers = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.shulkers");
    private final SelectSetting.Value signs = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.signs");
    private final SelectSetting.Value spawners = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.spawners");
    private final SelectSetting.Value banners = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.banners");
    private final SelectSetting.Value bells = new SelectSetting.Value(this.tileEntities, "modules.settings.norender.tile_entities.bells");

    // Частицы
    private final SelectSetting particles = new SelectSetting(this, "modules.settings.norender.particles");
    private final SelectSetting.Value totem = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.totem");
    private final SelectSetting.Value potions = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.potions");
    private final SelectSetting.Value bubbles = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.bubbles");
    private final SelectSetting.Value fireworks = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.fireworks");
    private final SelectSetting.Value explosions = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.explosions");
    private final SelectSetting.Value crits = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.crits");
    private final SelectSetting.Value blockBreak = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.break");
    private final SelectSetting.Value rain = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.rain");
    private final SelectSetting.Value elderGuardian = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.elder_guardian");
    private final SelectSetting.Value dragonBreath = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.dragon_breath");
    private final SelectSetting.Value hearts = new SelectSetting.Value(this.particles, "modules.settings.norender.particles.hearts");

    // Оверлеи экрана и окружения
    private final SelectSetting overlays = new SelectSetting(this, "modules.settings.norender.overlays");
    private final SelectSetting.Value fire = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.fire");
    private final SelectSetting.Value pumpkin = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.pumpkin");
    private final SelectSetting.Value portal = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.portal");
    private final SelectSetting.Value nausea = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.nausea");
    private final SelectSetting.Value blindness = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.blindness");
    private final SelectSetting.Value hurtCam = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.hurt_cam");
    private final SelectSetting.Value glint = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.glint");
    private final SelectSetting.Value fog = new SelectSetting.Value(this.overlays, "modules.settings.norender.overlays.fog");

    @Generated
    public SelectSetting getEntities() {
        return this.entities;
    }

    @Generated
    public SelectSetting.Value getPlayers() {
        return this.players;
    }

    @Generated
    public SelectSetting.Value getMobs() {
        return this.mobs;
    }

    @Generated
    public SelectSetting.Value getItems() {
        return this.items;
    }

    @Generated
    public SelectSetting.Value getArmorStands() {
        return this.armorStands;
    }

    @Generated
    public SelectSetting.Value getFallingBlocks() {
        return this.fallingBlocks;
    }

    @Generated
    public SelectSetting.Value getTnt() {
        return this.tnt;
    }

    @Generated
    public SelectSetting getTileEntities() {
        return this.tileEntities;
    }

    @Generated
    public SelectSetting.Value getChests() {
        return this.chests;
    }

    @Generated
    public SelectSetting.Value getEnderChests() {
        return this.enderChests;
    }

    @Generated
    public SelectSetting.Value getShulkers() {
        return this.shulkers;
    }

    @Generated
    public SelectSetting.Value getSigns() {
        return this.signs;
    }

    @Generated
    public SelectSetting.Value getSpawners() {
        return this.spawners;
    }

    @Generated
    public SelectSetting.Value getBanners() {
        return this.banners;
    }

    @Generated
    public SelectSetting.Value getBells() {
        return this.bells;
    }

    @Generated
    public SelectSetting getParticles() {
        return this.particles;
    }

    @Generated
    public SelectSetting.Value getTotem() {
        return this.totem;
    }

    @Generated
    public SelectSetting.Value getPotions() {
        return this.potions;
    }

    @Generated
    public SelectSetting.Value getBubbles() {
        return this.bubbles;
    }

    @Generated
    public SelectSetting.Value getFireworks() {
        return this.fireworks;
    }

    @Generated
    public SelectSetting.Value getExplosions() {
        return this.explosions;
    }

    @Generated
    public SelectSetting.Value getCrits() {
        return this.crits;
    }

    @Generated
    public SelectSetting.Value getBlockBreak() {
        return this.blockBreak;
    }

    @Generated
    public SelectSetting.Value getRain() {
        return this.rain;
    }

    @Generated
    public SelectSetting.Value getElderGuardian() {
        return this.elderGuardian;
    }

    @Generated
    public SelectSetting.Value getDragonBreath() {
        return this.dragonBreath;
    }

    @Generated
    public SelectSetting.Value getHearts() {
        return this.hearts;
    }

    @Generated
    public SelectSetting getOverlays() {
        return this.overlays;
    }

    @Generated
    public SelectSetting.Value getFire() {
        return this.fire;
    }

    @Generated
    public SelectSetting.Value getPumpkin() {
        return this.pumpkin;
    }

    @Generated
    public SelectSetting.Value getPortal() {
        return this.portal;
    }

    @Generated
    public SelectSetting.Value getNausea() {
        return this.nausea;
    }

    @Generated
    public SelectSetting.Value getBlindness() {
        return this.blindness;
    }

    @Generated
    public SelectSetting.Value getHurtCam() {
        return this.hurtCam;
    }

    @Generated
    public SelectSetting.Value getGlint() {
        return this.glint;
    }

    @Generated
    public SelectSetting.Value getFog() {
        return this.fog;
    }
}
