package org.scourge.terrain;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import org.scourge.Main;
import org.scourge.MovementListener;
import org.scourge.model.HasModel;
import org.scourge.model.Monster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: gabor
 * Date: Jun 15, 2010
 * Time: 8:17:17 AM
 */
public class MonsterGenerator extends Generator {
    private List<MonsterInstance> monsters = new ArrayList<MonsterInstance>();
    private Set<MonsterInstance> placed = new HashSet<MonsterInstance>();
    private Quaternion q = new Quaternion();
    private Quaternion q2 = new Quaternion();

    public MonsterGenerator(Region region, int x, int y, String monsterName) {
        super(region, x, y);
        if(monsterName != null) {
            for(int i = 0; i < 3; i++) {
                Monster monster = Monster.valueOf(monsterName);
                MonsterInstance monsterInstance = new MonsterInstance(monster);
                monsters.add(monsterInstance);
            }
        }
    }

    @Override
    public void generate() {
out:
        while(placed.size() < monsters.size()) {
            // put a new monster on the map
            for(MonsterInstance monsterInstance : monsters) {
                if(getRegion().findSpaceAround(getX(), getY(), monsterInstance.getCreatureModel().getNode())) {
                    placed.add(monsterInstance);
					// todo: not a good place for this...
					if(monsterInstance.getCreatureModel().getDebugNode() != null) {
						Main.getMain().getRoot().attachChild(monsterInstance.getCreatureModel().getDebugNode());
					}
                } else {
                    break out;
                }
            }
        }
    }

	@Override
	public void update(double tpf) {
        if(Main.getMain().isLoading()) return;

		// move around
        for(MonsterInstance monsterInstance : placed) {
            monsterInstance.getCreatureModel().moveToTopOfTerrain(tpf);
        }
    }

    @Override
    public void unloading() {
        Terrain terrain = Main.getMain().getTerrain();
        for(MonsterInstance monsterInstance : placed) {
            terrain.getNode().detachChild(monsterInstance.getCreatureModel().getNode());
        }
        monsters.clear();
        placed.clear();
//        terrain.getNode().updateRenderState();
//        terrain.getNode().updateWorldData(0);
//        terrain.getNode().updateModelBound();
//        terrain.getNode().updateWorldBound();
    }

    private class MonsterInstance implements HasModel, MovementListener {
        private Monster monster;
        private CreatureModel model;

        public MonsterInstance(Monster monster) {
            this.monster = monster;
            this.model = monster.createModel();
			this.model.setMovementListener(this);
			this.model.setMoving(true);
			this.model.setMovementSpeed(monster.getSpeed());
			this.model.setBounce(false);
//            this.model.setKeyFrame(Md2Model.Md2Key.run);
        }

        @Override
        public CreatureModel getCreatureModel() {
            return model;
        }

        public Monster getMonster() {
            return monster;
        }

		@Override
		public void moved() {
			// great!
		}

		@Override
		public void stopped() {
			q2.fromRotationMatrix(model.getNode().getRotation());
			q.fromAngleAxis(MathUtils.DEG_TO_RAD * (180f + (float)(90f * Math.random()) - 45f), Vector3.UNIT_Y);
			q2.multiplyLocal(q);
			model.getNode().setRotation(q2);
		}
	}
}

