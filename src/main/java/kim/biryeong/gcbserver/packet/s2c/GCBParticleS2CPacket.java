package kim.biryeong.gcbserver.packet.s2c;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public record GCBParticleS2CPacket(
        String particle,
        Vec offset,
        int count,
        double speed,
        boolean force,
        String data,
        ShapeData shape
) implements GCBS2CPacket {
    public static final String ID = "PARTICLE";

    public GCBParticleS2CPacket {
        Objects.requireNonNull(particle, "particle");
        Objects.requireNonNull(offset, "offset");
        data = Objects.requireNonNullElse(data, "");
        Objects.requireNonNull(shape, "shape");
    }

    @Override
    public String encode() {
        return ID + ":" + particle + ":" + offset.encode() + ":" + count + ":" + speed + ":" + force + ":" + data + ":" + shape.encode();
    }

    public record Vec(double x, double y, double z) {
        public static final Vec ZERO = new Vec(0.0d, 0.0d, 0.0d);
        public static final Vec UNIT_X = new Vec(1.0d, 0.0d, 0.0d);
        public static final Vec UNIT_Y = new Vec(0.0d, 1.0d, 0.0d);
        public static final Vec UNIT_Z = new Vec(0.0d, 0.0d, 1.0d);

        public String encode() {
            return x + "," + y + "," + z;
        }
    }

    public record ShapeOptions(
            double expand,
            double time,
            Vec matrixX,
            Vec matrixY,
            Vec matrixZ,
            UUID target
    ) {
        public static final ShapeOptions DEFAULT = new ShapeOptions(
                0.0d,
                0.0d,
                Vec.UNIT_X,
                Vec.UNIT_Y,
                Vec.UNIT_Z,
                null
        );

        public ShapeOptions {
            Objects.requireNonNull(matrixX, "matrixX");
            Objects.requireNonNull(matrixY, "matrixY");
            Objects.requireNonNull(matrixZ, "matrixZ");
        }

        public String encode() {
            return expand + ":" + time + ":" + matrixX.encode() + ":" + matrixY.encode() + ":" + matrixZ.encode() + ":" + (target == null ? "0" : target);
        }
    }

    public interface ShapeData {
        String encode();
    }

    public record Circle(Vec axis, Vec rotate, int points, double angle, ShapeOptions options, Vec... positions) implements ShapeData {
        public Circle {
            Objects.requireNonNull(axis, "axis");
            Objects.requireNonNull(rotate, "rotate");
            options = Objects.requireNonNullElse(options, ShapeOptions.DEFAULT);
            positions = positions == null ? new Vec[0] : positions;
        }

        @Override
        public String encode() {
            return join("circle", axis, rotate, points, angle, options, positions);
        }
    }

    public record Line(double period, ShapeOptions options, Vec... positions) implements ShapeData {
        public Line {
            options = Objects.requireNonNullElse(options, ShapeOptions.DEFAULT);
            positions = positions == null ? new Vec[0] : positions;
        }

        @Override
        public String encode() {
            return join("line", period, options, positions);
        }
    }

        public record Trail(double period, ShapeOptions options, Vec... positions) implements ShapeData {
        public Trail {
            options = Objects.requireNonNullElse(options, ShapeOptions.DEFAULT);
            positions = positions == null ? new Vec[0] : positions;
        }

        @Override
        public String encode() {
            return join("trail", period, options, positions);
        }
    }

    public record Sphere(Vec axis, int points, ShapeOptions options, Vec... positions) implements ShapeData {
        public Sphere {
            Objects.requireNonNull(axis, "axis");
            options = Objects.requireNonNullElse(options, ShapeOptions.DEFAULT);
            positions = positions == null ? new Vec[0] : positions;
        }

        @Override
        public String encode() {
            return join("sphere", axis, points, options, positions);
        }
    }

    public record Spiral(
            Vec axis,
            Vec rotate,
            int points,
            double period,
            double increment,
            boolean converge,
            ShapeOptions options,
            Vec... positions
    ) implements ShapeData {
        public Spiral {
            Objects.requireNonNull(axis, "axis");
            Objects.requireNonNull(rotate, "rotate");
            options = Objects.requireNonNullElse(options, ShapeOptions.DEFAULT);
            positions = positions == null ? new Vec[0] : positions;
        }

        @Override
        public String encode() {
            return join("spiral", axis, rotate, points, period, increment, converge, options, positions);
        }
    }

    public record SpiralLine(
            double period,
            double radius,
            int points,
            double startDegree,
            boolean inverse,
            ShapeOptions options,
            Vec... positions
    ) implements ShapeData {
        public SpiralLine {
            options = Objects.requireNonNullElse(options, ShapeOptions.DEFAULT);
            positions = positions == null ? new Vec[0] : positions;
        }

        @Override
        public String encode() {
            return join("spiralline", period, radius, points, startDegree, inverse, options, positions);
        }
    }

    private static String join(String id, Object... values) {
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(id);
        for (Object value : values) {
            if (value instanceof Vec[] positions) {
                for (Vec position : positions) {
                    joiner.add(position.encode());
                }
            } else if (value instanceof Vec vec) {
                joiner.add(vec.encode());
            } else if (value instanceof ShapeOptions options) {
                joiner.add(options.encode());
            } else {
                joiner.add(String.valueOf(value));
            }
        }
        return joiner.toString();
    }
}
