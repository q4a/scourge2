package org.scourge.terrain;

import com.ardor3d.math.Vector3;

/**
* User: gabor
* Date: Feb 7, 2010
* Time: 5:46:11 PM
*/
public enum Direction {
    NORTH(-90, new Vector3(0, 0, -1)) {
        public Direction opposite() {
            return SOUTH;
        }

        @Override
        public Direction next() {
            return WEST;
        }
    },
    EAST(0, new Vector3(-1, 0, 0)) {
        public Direction opposite() {
            return WEST;
        }

        @Override
        public Direction next() {
            return NORTH;
        }
    },
    SOUTH(90, new Vector3(0, 0, 1)) {
        public Direction opposite() {
            return NORTH;
        }

        @Override
        public Direction next() {
            return EAST;
        }
    },
    WEST(180, new Vector3(1, 0, 0)) {
       public Direction opposite() {
            return EAST;
        }

        @Override
        public Direction next() {
            return SOUTH;
        }
    };

    private float angle;
    private Vector3 dirVector;

    Direction(float angle, Vector3 dirVector) {
        this.angle = angle;
        this.dirVector = dirVector;
    }

    public float getAngle() {
        return angle;
    }

    public Vector3 getDirVector() {
        return dirVector;
    }

    public abstract Direction opposite();

    public abstract Direction next();
}
