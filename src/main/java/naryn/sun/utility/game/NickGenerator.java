package naryn.sun.utility.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Генератор случайных никнеймов.
 * Вынесен из TextUtility, чтобы разгрузить утилитный класс от ~430 строк данных.
 */
public final class NickGenerator {

    private static final Random random = new Random();

    private static final List<String> prefixes = Arrays.asList(
       "The", "Super", "Mega", "Ultra", "Power", "Master", "Great", "Hyper",
       "Quantum", "Atomic", "Cosmic", "Turbo", "Mighty", "Fantastic", "Legendary",
       "Epic", "Glorious", "Incredible", "Marvelous", "Supreme", "Stellar",
       "Dynamic", "Heroic", "Valiant", "Brave", "Noble", "Radiant", "Brilliant",
       "Bold", "Fearless", "Fierce", "Savage", "Infinite", "Storm", "Thunder",
       "Lightning", "Solar", "Lunar", "Galactic", "Nebula", "Phoenix", "Titan",
       "Colossal", "Majestic", "Regal", "Royal", "Sovereign", "Auroral", "Divine",
       "Ethereal", "Fiery", "Flaming", "Gigahertz", "Hypersonic", "Infernal",
       "Jovial", "Kaleidoscopic", "Luminous", "Magnetic", "Nebulous", "Olympian",
       "Pulsar", "Quasar", "Radiant", "Spectral", "Stellar", "Tachyon", "Umbra",
       "Vortex", "Warp", "Xenon", "Yellowstone", "Zephyr", "Alena", "Karina", "Eva"
    );

    private static final List<String> adjectives = Arrays.asList(
       "Swift", "Fierce", "Sneaky", "Brave", "Savage", "Fearless", "Stealthy",
       "Valiant", "Bold", "Cunning", "Mighty", "Noble", "Resolute", "Vigilant",
       "Relentless", "Intrepid", "Daring", "Gallant", "Tenacious", "Ferocious",
       "Unyielding", "Audacious", "Courageous", "Indomitable", "Dauntless",
       "Unstoppable", "Determined", "Invincible", "Unbreakable", "Epic",
       "Legendary", "Mythic", "Heroic", "Glorious", "Triumphant", "Fearsome",
       "Imposing", "Stalwart", "Stout", "Steadfast", "Grim", "Resolute",
       "Fateful", "Loyal", "Trusty", "Staunch", "Hardy", "Doughty", "Unflinching",
       "Unfaltering", "Brisk", "Keen", "Alert", "Quick", "Agile", "Nimble",
       "Lithe", "Spry", "Energetic", "Vibrant", "Dynamic", "Lively", "Sprightly",
       "Active", "Forceful", "Vigorous", "Spirited", "Animated", "Robust",
       "Brawny", "Muscular", "Husky", "Strong", "Tough", "Solid", "Sturdy",
       "Hefty", "Powerful", "Mighty", "Colossal", "Gigantic", "Mammoth", "Titanic",
       "Towering", "Massive", "Monumental", "Heroic", "Bravehearted", "Gutsy",
       "Doughty", "Unyielding", "Unwavering", "Ironwilled", "Strong-willed",
       "Unshakeable", "Elfie"
    );

    private static final List<String> animals = Arrays.asList(
       "Wolf", "Tiger", "Lion", "Eagle", "Panther", "Dragon", "Phoenix", "Bear",
       "Leopard", "Hawk", "Falcon", "Cheetah", "Jaguar", "Griffin", "Raven",
       "Fox", "Shark", "Viper", "Cobra", "Falcon", "Crocodile", "Raptor",
       "Condor", "Lynx", "Ocelot", "Cougar", "Puma", "Hound", "Bison", "Mammoth",
       "Rhino", "Buffalo", "Stallion", "Mustang", "Pegasus", "Wyvern", "Cerberus",
       "Minotaur", "Chimera", "Hydra", "Kraken", "Basilisk", "Manticore",
       "Unicorn", "Sphinx", "Grizzly", "Kodiak", "Polar Bear", "Sabertooth",
       "Direwolf", "Orca", "Narwhal", "Walrus", "Beluga", "Elephant", "Hippo",
       "Gorilla", "Orangutan", "Chimpanzee", "Baboon", "Mongoose", "Ferret",
       "Weasel", "Otter", "Badger", "Wolverine", "Honey Badger", "Lizard",
       "Iguana", "Gecko", "Komodo Dragon", "Monitor Lizard", "Tortoise", "Turtle",
       "Alligator", "Caiman", "Anaconda", "Python", "Boa", "Eel", "Swordfish",
       "Marlin", "Barracuda", "Piranha", "Penguin", "Albatross", "Seagull",
       "Pelican", "Stork", "Heron", "Flamingo", "MasTyp6ek", "Masha", "Tigr",
       "Legacy", ""
    );

    private static final List<String> suffixes = Arrays.asList(
       "Gamer", "Player", "Ninja", "Warrior", "Champion", "Legend", "Hero",
       "Master", "Conqueror", "Slayer", "Guardian", "Knight", "Paladin",
       "Crusader", "Ranger", "Assassin", "Mage", "Sorcerer", "Wizard",
       "Enchanter", "Necromancer", "Berserker", "Gladiator", "Samurai", "Viking",
       "Pirate", "Outlaw", "Mercenary", "Hunter", "Scout", "Rogue", "Thief",
       "Sentinel", "Protector", "Savior", "Defender", "Avenger", "Warlord",
       "Commander", "Captain", "General", "Marshal", "Overlord", "Monarch",
       "Emperor", "King", "Queen", "Prince", "Princess", "Duke", "Duchess",
       "Baron", "Baroness", "Lord", "Lady", "Warden", "Sentinel", "Crusader",
       "Champion", "Virtuoso", "Adept", "Prodigy", "Savant", "Genius", "Maven",
       "Whiz", "Ace", "Virtuoso", "Expert", "Specialist", "Technician",
       "Strategist", "Tactician", "Operative", "Agent", "Spy", "Infiltrator",
       "Saboteur", "Shadow", "Phantom", "Specter", "Shade", "Mystic", "Seer",
       "Oracle", "Prophet", "Visionary", "Dreamer", "Illusionist", "Conjurer",
       "Invoker", "Diviner", "Alchemist", "Shaman", "Druid", "Elementalist",
       "Geomancer", "Pyromancer", "Hydromancer", "Aeromancer", "Archon",
       "Brawler", "Catalyst", "Dynamo", "Energizer", "Flux", "Fusion", "Gizmo",
       "Hacker", "Innovator", "Juggernaut", "Kinetix", "Luminary", "Marauder",
       "Nomad", "Operator", "Pioneer", "Quickshot", "Rascal", "Slasher", "Titan",
       "Umbra", "Vanguard", "Warden", "Pro", "Xenon", "Yokai", "Zealot", "Zorro",
       "Zoltar"
    );

    /**
     * Генерирует случайный никнейм из комбинации prefix + adjective + animal + suffix + цифры.
     * Результат не длиннее 16 символов.
     */
    public static String generate() {
        String prefix = getRandomElement(prefixes);
        String adjective = getRandomElement(adjectives);
        String animal = getRandomElement(animals);
        String suffix = getRandomElement(suffixes);
        String year = random.nextInt(100) < 30 ? String.valueOf(2000 + random.nextInt(26)) : "";

        List<String> parts = new ArrayList<>();
        if (random.nextBoolean()) parts.add(prefix);
        if (random.nextBoolean()) parts.add(adjective);
        if (random.nextBoolean()) parts.add(animal);
        if (random.nextBoolean()) parts.add(suffix);

        if (parts.isEmpty()) parts.add(prefix);
        if (parts.size() < 2) parts.add(random.nextBoolean() ? adjective : animal);

        String nickname = String.join("", parts) + year;
        if (random.nextInt(100) < 20) {
            nickname = nickname + (random.nextBoolean() ? "52" : "69");
        } else {
            nickname = nickname + generateNumbers(2 + random.nextInt(3));
        }

        if (nickname.length() > 16) {
            nickname = nickname.substring(nickname.length() - 16);
        }
        return nickname;
    }

    private static String getRandomElement(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }

    private static String generateNumbers(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private NickGenerator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
