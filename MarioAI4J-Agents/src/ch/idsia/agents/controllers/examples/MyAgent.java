package ch.idsia.agents.controllers.examples;

import java.awt.Graphics;

import ch.idsia.agents.AgentOptions;
import ch.idsia.agents.IAgent;
import ch.idsia.agents.controllers.MarioHijackAIBase;
import ch.idsia.benchmark.mario.MarioSimulator;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.VisualizationComponent;
import ch.idsia.benchmark.mario.engine.generalization.Enemy;
import ch.idsia.benchmark.mario.engine.generalization.Entity;
import ch.idsia.benchmark.mario.engine.generalization.EntityType;
import ch.idsia.benchmark.mario.engine.generalization.Tile;
import ch.idsia.benchmark.mario.engine.input.MarioInput;
import ch.idsia.benchmark.mario.engine.input.MarioKey;
import ch.idsia.benchmark.mario.environments.IEnvironment;
import ch.idsia.benchmark.mario.options.FastOpts;
import ch.idsia.tools.EvaluationInfo;
import java.util.ArrayList;

/**
 * Your custom agent! Feel free to fool around!
 *
 * @author Jakub 'Jimmy' Gemrot, gemrot@gamedev.cuni.cz
 */
public class MyAgent extends MarioHijackAIBase implements IAgent {

    private boolean shooting = false;

    @Override
    public void reset(AgentOptions options) {
        super.reset(options);
    }

    @Override
    public void debugDraw(VisualizationComponent vis, LevelScene level, IEnvironment env, Graphics g) {
        super.debugDraw(vis, level, env, g);
        // provide custom visualization using 'g'
    }

    interface IChecker {

        boolean check(int x, int y);
    }

    enum Direction {

        Any,
        Forward,
        Backward,
    };

    class ObstacleCheck implements IChecker {

        @Override
        public boolean check(int x, int y) {
            return !t.emptyTile(x, y);
        }
    }
    
    class TileCheck implements IChecker {

        Tile tile;

        public TileCheck(Tile tile) {
            this.tile = tile;
        }

        @Override
        public boolean check(int x, int y) {
            return t.tile(x, y) == this.tile;
        }
    }

    class EmptyGroundCheck extends TileCheck implements IChecker {

        public EmptyGroundCheck() {
            super(Tile.NOTHING);
        }
    }

    class GroundCheck extends TileCheck implements IChecker {

        public GroundCheck() {
            super(Tile.BORDER_CANNOT_PASS_THROUGH);
        }
    }

    class FlowerPotCheck extends TileCheck implements IChecker {

        public FlowerPotCheck() {
            super(Tile.FLOWER_POT);
        }
    }

    class EnemyCheck implements IChecker {

        Direction dir;
        EntityType enemy;

        public EnemyCheck(EntityType enemy, Direction direction) {
            this.dir = direction;
            this.enemy = enemy;
        }

        @Override
        public boolean check(int x, int y) {
            for (Entity entity : e.entities(x, y)) {
                if (entity.type == enemy) {
                    if (dir == Direction.Any) {
                        return true;
                    } else if (dir == Direction.Forward && entity.speed.x > 0) {
                        return true;
                    } else if (dir == Direction.Backward && entity.speed.x < 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    class BoombaCheck extends EnemyCheck implements IChecker {

        public BoombaCheck(Direction direction) {
            super(EntityType.GOOMBA, direction);
        }
    }

    class SpikyCheck extends EnemyCheck implements IChecker {

        public SpikyCheck(Direction direction) {
            super(EntityType.SPIKY, direction);
        }
    }

    class FlowerCheck extends EnemyCheck implements IChecker {

        public FlowerCheck(Direction direction) {
            super(EntityType.ENEMY_FLOWER, direction);
        }
    }

    private boolean gridCheck(int x, int y, int xl, int yl, IChecker checker) {
        for (int i = 0; i <= xl; i++) {
            for (int j = 0; j <= yl; j++) {
                if (checker.check(x + i, y + j)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean goingToFall() {

        for (int i = 0; i <= 9; i++) {
            for (int j = -8; j <= 0; j++) {
                for (Entity entity : e.entities(i, j)) {
                    if (entity.speed.x < 0) {
                        if (gridCheck(1, j + 1, i - 1, 0, new EmptyGroundCheck())) {
                            if (!gridCheck(1, j, i - 1, 0, new ObstacleCheck())) {
                                return true;
                            }
                        }
                    } else if (entity.speed.x > 0) {
                        if (gridCheck(i, j + 1, 9 - i, 0, new EmptyGroundCheck())) {
                             if (!gridCheck(i, j, 9 - i, 0, new ObstacleCheck())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public MarioInput actionSelectionAI() {
        boolean onEdge = gridCheck(1, 1, 0, 0, new EmptyGroundCheck());
        boolean runRight = true;

        action.release(MarioKey.LEFT);

        if (goingToFall()
                //                && !gridCheck(1, -2, 2, 2, new BoombaCheck(Direction.Backward))
                //                && !gridCheck(1, -2, 2, 2, new SpikyCheck(Direction.Backward))
                && !gridCheck(-3, -2, 3, 3, new BoombaCheck(Direction.Forward))
                && !gridCheck(-3, -2, 3, 3, new SpikyCheck(Direction.Forward))
                && (!gridCheck(0, -2, 2, 5, new BoombaCheck(Direction.Any)) || (t.emptyTile(1, 1) && t.brick(0, 1)))
                && (!gridCheck(0, -2, 2, 5, new SpikyCheck(Direction.Any)) || (t.emptyTile(1, 1) && t.brick(0, 1)))
                && (!gridCheck(0, 1, 2, 1, new SpikyCheck(Direction.Any)) || (t.emptyTile(1, 1) && t.brick(0, 1)))) {

            if (onEdge || gridCheck(0, 0, 2, 0, new GroundCheck())) {
                action.press(MarioKey.LEFT);
                action.release(MarioKey.RIGHT);
            } else {
                action.release(MarioKey.RIGHT);
            }
            return action;
        }

        action.set(MarioKey.RIGHT, runRight);

        boolean wantToJump = false;
        wantToJump |= gridCheck(1, -2, 3, 2, new GroundCheck());
        wantToJump &= mario.speed.x > 2 || !onEdge;

//        wantToJump |= gridCheck(1, 1, 4, 0, new EmptyGroundCheck()) && gridCheck(6, 1, 0, 0, new GroundCheck()); // long jump over a 5-hole
        wantToJump |= gridCheck(0, 0, 1, 0, new SpikyCheck(Direction.Forward));
        wantToJump |= gridCheck(0, 0, 1, 0, new SpikyCheck(Direction.Backward));
        wantToJump |= gridCheck(4, 3, 0, 0, new SpikyCheck(Direction.Backward)) && onEdge && !(t.brick(3, 3) || t.brick(2, 3));
        wantToJump |= gridCheck(0, 2, 3, 0, new SpikyCheck(Direction.Backward)) && onEdge;
        wantToJump |= gridCheck(3, 2, 0, 0, new SpikyCheck(Direction.Backward)) && onEdge;
        wantToJump |= gridCheck(1, 1, 1, 0, new SpikyCheck(Direction.Any)) && onEdge;
        wantToJump |= gridCheck(1, 2, 1, 0, new SpikyCheck(Direction.Any)) && onEdge;
        wantToJump |= gridCheck(1, 2, 2, 0, new SpikyCheck(Direction.Backward)) && onEdge;
        wantToJump |= gridCheck(1, 3, 3, 0, new SpikyCheck(Direction.Backward)) && onEdge && !(t.brick(3, 3) || t.brick(2, 3));
        wantToJump |= gridCheck(1, 3, 1, 0, new SpikyCheck(Direction.Forward)) && onEdge;
        wantToJump |= gridCheck(1, 3, 3, 0, new SpikyCheck(Direction.Forward)) && onEdge && (t.brick(4, 3) || t.brick(5, 3));
        wantToJump |= gridCheck(1, 1, 1, 0, new SpikyCheck(Direction.Backward)) && onEdge;
//        wantToJump |= gridCheck(3, 2, 1, 0, new SpikyCheck(Direction.Backward)) && gridCheck(5, 2, 1, 0, new BoombaCheck(Direction.Forward));

        wantToJump |= gridCheck(1, 0, 0, 0, new BoombaCheck(Direction.Any));
        wantToJump |= gridCheck(3, 0, 1, 0, new BoombaCheck(Direction.Backward)) && !gridCheck(2, 0, 1, 0, new SpikyCheck(Direction.Forward));
        wantToJump |= gridCheck(1, 0, 2, 0, new BoombaCheck(Direction.Backward)) && !gridCheck(2, 0, 1, 0, new SpikyCheck(Direction.Forward));
        wantToJump |= gridCheck(1, 1, 1, 0, new BoombaCheck(Direction.Backward)) && onEdge;
        wantToJump |= gridCheck(3, 3, 1, 0, new BoombaCheck(Direction.Backward)) && onEdge;
//        wantToJump |= gridCheck(1, 1, 1, 0, new BoombaCheck(Direction.Any)) && onEdge;
        wantToJump |= gridCheck(2, 2, 1, 0, new BoombaCheck(Direction.Backward)) && onEdge;

        wantToJump |= gridCheck(-1, 0, 1, 0, new BoombaCheck(Direction.Forward));
        wantToJump |= gridCheck(-1, 0, 1, 0, new SpikyCheck(Direction.Forward));

        wantToJump |= gridCheck(1, -1, 0, 1, new FlowerPotCheck()) && !gridCheck(1, -4, 0, 3, new FlowerCheck(Direction.Any)); // 2 tile flowerpot
        wantToJump |= gridCheck(1, -2, 0, 0, new FlowerPotCheck()) && !gridCheck(1, -5, 0, 3, new FlowerCheck(Direction.Any)); // 3 tile flowerpot

//        wantToJump |= spikyInHole(1, 1, 3, 5) && !t.brick(1, 1);
        wantToJump &= mario.mayJump; // WARNING: do not press JUMP if UNABLE TO JUMP!
        wantToJump &= mario.speed.x > 1 || !t.emptyTile(1, 0);

        action.set(MarioKey.JUMP, wantToJump);

        boolean keepJumping = false;
//        keepJumping |= spikyInHole(-1, -1, 3, 5);
//        keepJumping |= spikyInHole(1, 1, 1, 5);
//        keepJumping |= enemyAhead() || brickAhead(); // ENEMY || BRICK AHEAD => JUMP

        keepJumping |= gridCheck(3, 1, 1, 0, new BoombaCheck(Direction.Backward)); // jump on goomba walking on the cliff 

        keepJumping |= gridCheck(0, 0, 2, 1, new SpikyCheck(Direction.Forward));

//        keepJumping &= !gridCheck(2, 0, 1, 0, new BoombaCheck(Direction.Any)); // fall an goomba when he is on the same level
        keepJumping &= !gridCheck(2, 0, 1, 0, new BoombaCheck(Direction.Backward));
        
        keepJumping |= gridCheck(3, 2, 1, 1, new BoombaCheck(Direction.Forward)) && (t.brick(5, 2) || t.brick(6, 2));

        keepJumping |= gridCheck(1, 2, 2, 3, new SpikyCheck(Direction.Forward));
        keepJumping |= gridCheck(0, 3, 2, 0, new SpikyCheck(Direction.Forward));
        
        keepJumping |= gridCheck(1, 3, 3, 3, new SpikyCheck(Direction.Any));
        keepJumping |= gridCheck(1, 0, 2, 2, new SpikyCheck(Direction.Backward));// && mario.speed.x > 3;
//        keepJumping |= gridCheck(2, 0, 1, 2, new SpikyCheck(Direction.Forward));

        keepJumping |= gridCheck(0, 4, 1, 1, new SpikyCheck(Direction.Any)) && (gridCheck(3, 4, 2, 1, new BoombaCheck(Direction.Forward))
                || gridCheck(3, 4, 2, 1, new SpikyCheck(Direction.Forward)));
        
//        keepJumping |= gridCheck(3, 0, 2, 0, new GroundCheck());
        keepJumping |= gridCheck(0, 2, 1, 0, new SpikyCheck(Direction.Forward));

        keepJumping |= gridCheck(1, -1, 0, 1, new FlowerPotCheck());

        keepJumping |= gridCheck(1, -2, 3, 3, new GroundCheck());

        keepJumping &= !mario.onGround;

        if (keepJumping) {
            action.press(MarioKey.JUMP);
        }

        return action;
    }

    public static void main(String[] args) {
        boolean test = true;

        if (!test) {
            while (true) {
                String options = FastOpts.FAST_VISx2_02_JUMPING
                        + FastOpts.L_ENEMY(Enemy.GOOMBA, Enemy.SPIKY)
                        + FastOpts.L_TUBES_ON
                        + FastOpts.L_RANDOM_SEED(0) //                    + FastOpts.L_RANDOMIZE
                        ;

                MarioSimulator simulator = new MarioSimulator(options);

                IAgent agent = new MyAgent();

                simulator.run(agent);
            }
        } else {

            int wins = 0;
            ArrayList<Integer> losts = new ArrayList<Integer>();
            for (int i = 0; i < 1000; i++) {
                String options = FastOpts.FAST_VISx2_02_JUMPING + FastOpts.L_ENEMY(Enemy.GOOMBA, Enemy.SPIKY) + FastOpts.L_TUBES_ON
                        + FastOpts.L_RANDOM_SEED(i)
                        + FastOpts.VIS_OFF;
                MarioSimulator simulator = new MarioSimulator(options);
                IAgent agent = new MyAgent();
                EvaluationInfo info = simulator.run(agent);
                if (info.marioStatus == 1) {
                    wins++;
                } else {
                    losts.add(i);
                }
            }
            System.out.println("WINS: ");
            System.out.println(wins);
            System.out.println(losts);
        }
    }
}
