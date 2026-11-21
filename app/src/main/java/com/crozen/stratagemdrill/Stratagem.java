package com.crozen.stratagemdrill;

/** One call-in code: a name, an icon and a sequence of arrows. */
final class Stratagem {

    static final int U = 0, D = 1, L = 2, R = 3;

    enum Kind { STRIKE, LASER, BOMB, SMOKE, SUPPLY, WEAPON, SHIELD, TURRET, MINES, BEACON, REINFORCE, NUKE }

    static final int RED = 0xFFE8513A;
    static final int BLUE = 0xFF41A8F0;
    static final int GREEN = 0xFF63C23A;
    static final int YELLOW = 0xFFFFE21A;

    final String name;
    final Kind kind;
    final int color;
    final int[] code;

    private Stratagem(String name, Kind kind, int color, String code) {
        this.name = name;
        this.kind = kind;
        this.color = color;
        this.code = new int[code.length()];
        for (int i = 0; i < code.length(); i++) {
            switch (code.charAt(i)) {
                case 'U': this.code[i] = U; break;
                case 'D': this.code[i] = D; break;
                case 'L': this.code[i] = L; break;
                default:  this.code[i] = R; break;
            }
        }
    }

    /**
     * Original names and codes for this game. Edit freely: U/D/L/R = up/down/left/right,
     * up to 8 arrows per code.
     */
    static final Stratagem[] ALL = {
            // Offensive (red)
            new Stratagem("ORBITAL LANCE", Kind.LASER, RED, "RDRUL"),
            new Stratagem("SKYFALL BARRAGE", Kind.STRIKE, RED, "RRDLRD"),
            new Stratagem("PIN-POINT STRIKE", Kind.STRIKE, RED, "RUUD"),
            new Stratagem("CARPET RUN", Kind.BOMB, RED, "URRDDL"),
            new Stratagem("THUNDER DROP", Kind.BOMB, RED, "ULDRR"),
            new Stratagem("SMOKE CURTAIN", Kind.SMOKE, RED, "RULD"),
            new Stratagem("EMP PULSE", Kind.BEACON, RED, "RRLLU"),
            // Supply (blue)
            new Stratagem("SUPPLY CRATE", Kind.SUPPLY, BLUE, "DRUU"),
            new Stratagem("AMMO CACHE", Kind.SUPPLY, BLUE, "DDRL"),
            new Stratagem("RAILGUN M-7", Kind.WEAPON, BLUE, "DLRULR"),
            new Stratagem("FLAMECASTER", Kind.WEAPON, BLUE, "LDRDU"),
            new Stratagem("HEAVY LAUNCHER", Kind.WEAPON, BLUE, "DULRDR"),
            new Stratagem("BUBBLE DOME", Kind.SHIELD, BLUE, "DURLRU"),
            // Defensive (green)
            new Stratagem("AUTO SENTRY", Kind.TURRET, GREEN, "DULRR"),
            new Stratagem("GATLING NEST", Kind.TURRET, GREEN, "DURLD"),
            new Stratagem("MORTAR POST", Kind.TURRET, GREEN, "DRUDL"),
            new Stratagem("MINEFIELD", Kind.MINES, GREEN, "DLURD"),
            new Stratagem("TESLA PYLON", Kind.BEACON, GREEN, "DURLUD"),
            // Mission (yellow)
            new Stratagem("REINFORCE", Kind.REINFORCE, YELLOW, "ULDRU"),
            new Stratagem("DISTRESS BEACON", Kind.BEACON, YELLOW, "UDUR"),
            new Stratagem("SCANNER UPLINK", Kind.BEACON, YELLOW, "UULRD"),
            new Stratagem("EVAC FLARE", Kind.BEACON, YELLOW, "URDDU"),
            new Stratagem("NUKE CHARGE", Kind.NUKE, YELLOW, "DLUDRULD"),
    };
}
