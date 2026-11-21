package com.crozen.stratagemdrill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Game rules and state. No Android dependencies, so it is easy to test. */
final class Game {

    enum State { TITLE, ROUND_INTRO, PLAYING, ROUND_END, GAME_OVER }

    interface Events {
        void onArrowOk(int index);
        void onArrowWrong();
        void onStratagemDone();
        void onRoundStart();
        void onRoundComplete();
        void onTallyStep();
        void onGameOver(boolean newBest);
        void onLowTimeTick();
    }

    static final long INTRO_MS = 1800;
    static final long MAX_TIME_MS = 10_000;
    static final long TALLY_STEP_MS = 550;
    static final long ROUND_END_MS = 3400;
    static final long GAME_OVER_LOCK_MS = 900;

    private final Events ev;
    private final Random rng = new Random();

    State state = State.TITLE;
    long stateTime;
    int round;
    int score;
    int best;
    boolean newBest;

    final List<Stratagem> queue = new ArrayList<>();
    int queueIndex;
    int inputPos;
    long timeLeft;
    boolean perfect;

    long errorFlashMs;
    long completeFlashMs;
    private long lockMs;
    private long lastLowTick;

    int bonusRound, bonusTime, bonusPerfect;
    int tallyStep;

    Game(Events ev, int best) {
        this.ev = ev;
        this.best = best;
    }

    Stratagem current() {
        return queueIndex < queue.size() ? queue.get(queueIndex) : null;
    }

    // ---- input ----

    void input(int dir) {
        switch (state) {
            case TITLE:
                startGame();
                break;
            case GAME_OVER:
                if (stateTime >= GAME_OVER_LOCK_MS) startGame();
                break;
            case PLAYING:
                handleArrow(dir);
                break;
            default:
                break;
        }
    }

    void tap() {
        if (state == State.TITLE || (state == State.GAME_OVER && stateTime >= GAME_OVER_LOCK_MS)) {
            startGame();
        }
    }

    private void handleArrow(int dir) {
        Stratagem s = current();
        if (s == null || lockMs > 0) return;
        if (s.code[inputPos] == dir) {
            inputPos++;
            score += 5;
            ev.onArrowOk(inputPos - 1);
            if (inputPos == s.code.length) {
                score += s.code.length * 10;
                timeLeft = Math.min(MAX_TIME_MS, timeLeft + timeBonusMs());
                completeFlashMs = 260;
                queueIndex++;
                inputPos = 0;
                if (queueIndex >= queue.size()) {
                    enterRoundEnd();
                } else {
                    ev.onStratagemDone();
                }
            }
        } else {
            inputPos = 0;
            perfect = false;
            errorFlashMs = 320;
            lockMs = 150;
            ev.onArrowWrong();
        }
    }

    // ---- flow ----

    private void startGame() {
        score = 0;
        newBest = false;
        startRound(1);
    }

    private void startRound(int r) {
        round = r;
        buildQueue();
        queueIndex = 0;
        inputPos = 0;
        perfect = true;
        timeLeft = MAX_TIME_MS;
        errorFlashMs = 0;
        completeFlashMs = 0;
        lockMs = 0;
        enter(State.ROUND_INTRO);
        ev.onRoundStart();
    }

    private void buildQueue() {
        queue.clear();
        int count = Math.min(3 + round, 12);
        int maxLen = Math.min(3 + round, 8);
        List<Stratagem> pool = new ArrayList<>();
        for (Stratagem s : Stratagem.ALL) if (s.code.length <= maxLen) pool.add(s);
        Stratagem prev = null;
        for (int i = 0; i < count; i++) {
            Stratagem s;
            do {
                s = pool.get(rng.nextInt(pool.size()));
            } while (s == prev && pool.size() > 1);
            queue.add(s);
            prev = s;
        }
    }

    private long timeBonusMs() {
        return Math.max(1200, 3000 - (round - 1) * 150L);
    }

    private void enterRoundEnd() {
        bonusRound = 50 + 25 * round;
        bonusTime = (int) (timeLeft / 100);
        bonusPerfect = perfect ? 100 : 0;
        tallyStep = 0;
        enter(State.ROUND_END);
        ev.onRoundComplete();
    }

    private void gameOver() {
        newBest = score > best;
        if (newBest) best = score;
        enter(State.GAME_OVER);
        ev.onGameOver(newBest);
    }

    private void enter(State s) {
        state = s;
        stateTime = 0;
        lastLowTick = -1000;
    }

    // ---- tick ----

    void update(long dt) {
        stateTime += dt;
        errorFlashMs = Math.max(0, errorFlashMs - dt);
        completeFlashMs = Math.max(0, completeFlashMs - dt);
        lockMs = Math.max(0, lockMs - dt);

        switch (state) {
            case ROUND_INTRO:
                if (stateTime >= INTRO_MS) enter(State.PLAYING);
                break;
            case PLAYING:
                timeLeft -= dt;
                if (timeLeft <= 0) {
                    timeLeft = 0;
                    gameOver();
                } else if (timeLeft < MAX_TIME_MS / 4 && stateTime - lastLowTick >= 500) {
                    lastLowTick = stateTime;
                    ev.onLowTimeTick();
                }
                break;
            case ROUND_END:
                int step = (int) Math.min(3, stateTime / TALLY_STEP_MS);
                while (tallyStep < step) {
                    tallyStep++;
                    if (tallyStep == 1) score += bonusRound;
                    else if (tallyStep == 2) score += bonusTime;
                    else score += bonusPerfect;
                    ev.onTallyStep();
                }
                if (stateTime >= ROUND_END_MS) startRound(round + 1);
                break;
            default:
                break;
        }
    }

    float timeFraction() {
        return timeLeft / (float) MAX_TIME_MS;
    }
}
