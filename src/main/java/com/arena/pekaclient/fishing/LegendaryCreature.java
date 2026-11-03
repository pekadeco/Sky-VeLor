package com.arena.pekaclient.fishing;

/** Catch announcements for all tracked sea-creature rarities; the enum name is kept for save compatibility. */
public enum LegendaryCreature {
    // Common and early-game creatures. Kept in this enum so existing counter persistence remains compatible.
    SQUID("Squid", false, "A SQUID APPEARED", "YOU CAUGHT A LOWLY SQUID"),
    SEA_WALKER("Sea Walker", false, "YOU CAUGHT A SEA WALKER"),
    NIGHT_SQUID("Night Squid", false, "PITCH DARKNESS REVEALS A NIGHT SQUID",
            "PITCH DARKNESS REVEALS YOU'VE CAUGHT A NIGHT SQUID"),
    SEA_GUARDIAN("Sea Guardian", false, "YOU'VE STUMBLED UPON A SEA GUARDIAN",
            "YOU STUMBLED UPON A SEA GUARDIAN"),
    FROG_MAN("Frog Man", false, "IS IT A FROG? IS IT A MAN? WELL, YES, SORTA, IT'S FROG MAN"),
    TRASH_GOBBLER("Trash Gobbler", false, "THE TRASH GOBBLER IS HUNGRY FOR YOU"),
    FROZEN_STEVE("Frozen Steve", false, "FROZEN STEVE FELL INTO THE POND LONG AGO"),
    ATOLL_CROAKER("Atoll Croaker", false, "AN INQUISITIVE ATOLL CROAKER TAKES THE BAIT"),
    BOGGED("Bogged", false, "YOU'VE HOOKED A BOGGED"),
    HAGGARD("Haggard", false, "A HAGGARD STUMBLES TO THE SHORE"),
    JUMPIN_JACK("Jumpin' Jack", false, "WATCH OUT! IT'S JUMPIN' JACK"),
    FRIED_CHICKEN("Fried Chicken", false, "SMELLS OF BURNING. MUST BE A FRIED CHICKEN"),
    NURSE_SHARK("Nurse Shark", false, "A TINY FIN EMERGES FROM THE WATER, YOU'VE CAUGHT A NURSE SHARK"),

    // Every Uncommon, Rare and Epic creature is also counted in the shared Common Creature (CC) row.
    SEA_WITCH("Sea Witch", false, "IT LOOKS LIKE YOU'VE DISRUPTED THE SEA WITCH'S BREWING SESSION",
            "WATCH OUT, SHE'S FURIOUS"),
    SEA_ARCHER("Sea Archer", false, "YOU REELED IN A SEA ARCHER"),
    RIDER_OF_THE_DEEP("Rider of the Deep", false, "THE RIDER OF THE DEEP HAS EMERGED"),
    CATFISH("Catfish", false, "HUH? A CATFISH"),
    SEA_LEECH("Sea Leech", false, "GROSS! A SEA LEECH"),
    GUARDIAN_DEFENDER("Guardian Defender", false, "YOU'VE DISCOVERED A GUARDIAN DEFENDER OF THE SEA"),
    DEEP_SEA_PROTECTOR("Deep Sea Protector", false,
            "YOU HAVE AWOKEN THE DEEP SEA PROTECTOR, PREPARE FOR A BATTLE"),
    DUMPSTER_DIVER("Dumpster Diver", false, "A SWAMPY MASS OF SLIME EMERGES, THE DUMPSTER DIVER",
            "A DUMPSTER DIVER HAS EMERGED FROM THE SWAMP"),
    BANSHEE("Banshee", false, "THE DESOLATE WAIL OF A BANSHEE BREAKS THE SILENCE"),
    BAYOU_SLUDGE("Bayou Sludge", false, "A SWAMPY MASS OF SLIME EMERGES, THE BAYOU SLUDGE"),
    LOTUS_GUARDIAN("Lotus Guardian", false, "A LOTUS GUARDIAN EMERGES, READY TO PROTECT THE ATOLL"),
    GORF("gorF", false, "WHAT EVEN IS THAT?! A... GORF"),
    DROWNED_CAPTAIN("Drowned Captain", false, "A DROWNED CAPTAIN TAKES HOLD OF YOUR BOBBER"),
    WETWING("Wetwing", false, "LOOK! A WETWING EMERGES"),
    TADGANG("Tadgang", false, "A GANG OF LILTADS"),
    ENT("Ent", false, "YOU'VE HOOKED AN ENT, AS ANCIENT AS THE FOREST ITSELF"),
    STRIDERSURFER("Stridersurfer", false, "YOU CAUGHT A STRIDERSURFER"),
    BRINELING("Brineling", false, "A BRINELING INTERRUPTS YOU WITH A STREAM OF BUBBLES"),
    SPRAWL("Sprawl", false, "A SPRAWL EMERGES FROM THE BLUE, AND IT'S LOOKING FOR YOU"),
    TORRID("Torrid", false, "THE LAUGHTER OF A TORRID ECHOES THROUGH THE AIR"),
    MAGMA_SLUG("Magma Slug", false, "FROM BENEATH THE LAVA APPEARS A MAGMA SLUG"),
    MOOGMA("Moogma", false, "YOU HEAR A FAINT MOO FROM THE LAVA... A MOOGMA APPEARS"),
    LAVA_LEECH("Lava Leech", false, "A SMALL BUT FEARSOME LAVA LEECH EMERGES"),
    PYROCLASTIC_WORM("Pyroclastic Worm", false,
            "YOU FEEL THE HEAT RADIATING AS A PYROCLASTIC WORM SURFACES"),
    LAVA_FLAME("Lava Flame", false, "A LAVA FLAME FLIES OUT FROM BENEATH THE LAVA"),
    FIRE_EEL("Fire Eel", false, "A FIRE EEL SLITHERS OUT OF THE DEPTHS"),
    TAURUS("Taurus", false, "TAURUS AND HIS STEED EMERGE"),
    VOLCANIC_SNAIL("Volcanic Snail", false,
            "YOU FEEL A BURNING SENSATION AS YOU REEL IN A VOLCANIC SNAIL"),
    FIREPROOF_WITCH("Fireproof Witch", false, "TROUBLE'S BREWING, IT'S A FIREPROOF WITCH"),
    MAGMA_PILLAR("Magma Pillar", false, "A MAGMA PILLAR RISES FROM THE LAVA"),
    FROSTY("Frosty", false, "IT'S A SNOWMAN! HE LOOKS HARMLESS", "ITS A SNOWMAN! IT LOOKS HARMLESS"),
    GRINCH("Grinch", false, "THE GRINCH STOLE JERRY'S GIFTS... GET THEM BACK"),
    NUTCRACKER("Nutcracker", false, "YOU FOUND A FORGOTTEN NUTCRACKER LAYING BENEATH THE ICE"),
    SCARECROW("Scarecrow", false, "PHEW! IT'S ONLY A SCARECROW"),
    NIGHTMARE("Nightmare", false, "YOU HEAR TROTTING FROM BENEATH THE WAVES, YOU CAUGHT A NIGHTMARE"),
    WEREWOLF("Werewolf", false, "IT MUST BE A FULL MOON, A WEREWOLF APPEARS",
            "IT MUST BE A FULL MOON, IT'S A WEREWOLF"),
    BLUE_SHARK("Blue Shark", false, "YOU SPOT A FIN AS BLUE AS THE WATER IT CAME FROM, IT'S A BLUE SHARK"),
    TIGER_SHARK("Tiger Shark", false, "A STRIPED BEAST BOUNDS FROM THE DEPTHS, THE WILD TIGER SHARK"),
    INKLING("Inkling", false, "YOU GET AN INKLING YOU'VE CAUGHT... AN INKLING"),
    SNAPPING_TURTLE("Snapping Turtle", false,
            "A SNAPPING TURTLE IS COMING YOUR WAY, AND IT'S ANGRY"),
    MANTA_RAY("Manta Ray", false, "A MAJESTIC CREATURE RISES FROM THE WATER. IT'S A MANTA RAY"),
    MITHRIL_GRUBBER("Mithril Grubber", false,
            "A LEECH OF THE MINES SURFACES... YOU'VE CAUGHT A MITHRIL GRUBBER"),
    OASIS_SHEEP("Oasis Sheep", false, "AN OASIS SHEEP APPEARS FROM THE WATER"),
    OASIS_RABBIT("Oasis Rabbit", false, "AN OASIS RABBIT APPEARS FROM THE WATER"),
    CARROT_KING("Carrot King", false, "IS THAT EVEN A FISH? IT'S THE CARROT KING"),
    AGARIMOO("Agarimoo", false, "YOUR CHUMCAP BUCKET TREMBLES, IT'S AN AGARIMOO"),
    WATER_WORM("Water Worm", false, "A WATER WORM SURFACES"),
    POISONED_WATER_WORM("Poisoned Water Worm", false, "A POISONED WATER WORM SURFACES"),
    FLAMING_WORM("Flaming Worm", false, "A FLAMING WORM SURFACES FROM THE DEPTHS"),
    LAVA_BLAZE("Lava Blaze", false, "A LAVA BLAZE HAS SURFACED FROM THE DEPTHS",
            "A LAVA BLAZE HAS SURFACED FROM THE DEPTHS"),
    LAVA_PIGMAN("Lava Pigman", false, "A LAVA PIGMAN HAS SURFACED FROM THE DEPTHS",
            "A LAVA PIGMAN AROSE FROM THE DEPTHS"),

    THE_LOCH_EMPEROR("The Loch Emperor", "THE LOCH EMPEROR ARISES FROM THE DEPTHS",
            "THE SEA EMPEROR", "SEA EMPEROR", "A SEA EMPEROR"),
    WATER_HYDRA("Water Hydra", "THE WATER HYDRA HAS COME TO TEST YOUR STRENGTH",
            "THE WATER HYDRA HAS COME"),
    ALLIGATOR("Alligator", "A LONG SNOUT BREAKS THE SURFACE OF THE WATER"),
    ABYSSAL_MINER("Abyssal Miner", "AN ABYSSAL MINER BREAKS OUT OF THE WATER",
            "AN ABYSSAL MINER BREAKS"),
    BLUE_RINGED_OCTOPUS("Blue Ringed Octopus", "A GARISH SET OF TENTACLES ARISE"),
    FIERY_SCUTTLER("Fiery Scuttler", "A FIERY SCUTTLER INCONSPICUOUSLY WADDLES UP TO YOU"),
    GREAT_WHITE_SHARK("Great White Shark", "HIDE NO LONGER, A GREAT WHITE SHARK HAS TRACKED YOUR SCENT",
            "HIDE NO LONGER, A GREAT WHITE SHARK"),
    PHANTOM_FISHER("Phantom Fisher", "THE SPIRIT OF A LONG LOST PHANTOM FISHER"),
    PUDDLE_JUMPER("Puddle Jumper", "A PUDDLE JUMPER IS PREPARING FOR LIFTOFF"),
    SILKBREEZE("Silkbreeze", "SOMETHING ZIPS THROUGH THE AIR"),
    THUNDER("Thunder", "YOU HEAR A MASSIVE RUMBLE AS THUNDER EMERGES",
            "YOU HEAR A MASSIVE RUMBLE AS"),
    YETI("Yeti", "WHAT IS THIS CREATURE"),
    REINDRAKE("Reindrake", "A REINDRAKE FORMS FROM THE DEPTHS",
            "REINDRAKE FORMS FROM THE DEPTHS"),
    LORD_JAWBUS("Lord Jawbus", "YOU HAVE ANGERED A LEGENDARY CREATURE... LORD JAWBUS HAS ARRIVED");

    public final String displayName;
    /** True when this creature gets its own HUD row; false creatures share the Common Creature row. */
    public final boolean legendary;
    final String[] chatPrefixes;

    LegendaryCreature(String displayName, String chatPrefix, String... aliases) {
        this(displayName, true, chatPrefix, aliases);
    }

    LegendaryCreature(String displayName, boolean legendary, String chatPrefix, String... aliases) {
        this.displayName = displayName;
        this.legendary = legendary;
        this.chatPrefixes = new String[aliases.length + 1];
        this.chatPrefixes[0] = chatPrefix;
        System.arraycopy(aliases, 0, this.chatPrefixes, 1, aliases.length);
    }
}
