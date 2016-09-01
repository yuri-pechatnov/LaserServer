package com.pechatnov;

/**
 * Created by ura on 07.10.15.
 */

import java.io.IOException;
import java.util.LinkedList;
import org.apache.commons.math3.linear.*;

public class Draw {
    private Laser laser;
    boolean [][] matrix;
    protected int scale = 4;

    private boolean useTransform = false;

    private MImage image = null;

    private void openImage(String fname) {
        image = MImage.openImage(fname);
    }

    public void setScale(int scaleArg) {
        scale = scaleArg;
    }
    public Integer getScale() { return scale; }

    public boolean isUseTransform() {
        return useTransform;
    }

    public void setUseTransform(boolean useTransform) {
        this.useTransform = useTransform;
    }

    public Draw(Laser laser_) {
        laser = laser_;
        flushTransform();
    }

    static class Corner {
        public Double sx, sy, dx, dy; // Source and Destination coords
        /* source coords:
            (0, 0)   (1, 0)
            (0, -1)  (1, -1)
        * */
        Corner(Integer sx_, Integer sy_) {
            sx = sx_.doubleValue();
            sy = sy_.doubleValue();
            dx = sx;
            dy = sy;
        }
    }

    class TransformData {
        public Double a, b, A, B, C, D, E, F;
        public final Double c = 1.0;
        public Double transX(double sx, double sy) {
            return (A * sx + B * sy + C) / (a * sx + b * sy + c);
        }
        public Double transY(double sx, double sy) {
            return (D * sx + E * sy + F) / (a * sx + b * sy + c);
        }
        public Double[] transXY(double sx, double sy) {
            Double zn = (a * sx + b * sy + c);
            if (zn == 0)
                return new Double[] {0d, 0d};
            zn = 1d / zn;
            return new Double[] {(A * sx + B * sy + C) * zn, (D * sx + E * sy + F) / zn};
        }
    }

    TransformData transData = new TransformData(), revTransData = new TransformData();
    Corner corners[];

    public void fixCorner(Integer sx, Integer sy) {
        for (Corner corner : corners)
            if (sx.doubleValue() == corner.sx && sy.doubleValue() == corner.sy) {
                corner.dx = laser.getX().doubleValue();
                corner.dy = laser.getY().doubleValue();
            }
    }


    public TransformData calculateTransformData(boolean reverseTransformationFlag) {
        double[][] coef = new double[8][];
        double[] results = new double[8];
        for (int i = 0; i < 4; i++) {
            /* i * 2 - Xi coord; i * 2 + 1 - Yi coord */
            double  sx, sy, dx, dy;
            if (!reverseTransformationFlag) {
                sx = corners[i].sx; sy = corners[i].sy;
                dx = corners[i].dx; dy = corners[i].dy;
            } else {
                dx = corners[i].sx; dy = corners[i].sy;
                sx = corners[i].dx; sy = corners[i].dy;
            }
            coef[i * 2    ] = new double[] {
                    sx * dx, sy * dx, -sx, -sy, -1f,   0,   0,   0
            };
            results[i * 2    ] = -dx;
            coef[i * 2 + 1] = new double[] {
                    sx * dy, sy * dy,   0,   0,   0, -sx, -sy, -1f
            };
            results[i * 2 + 1] = -dy;
        }
        RealMatrix coefficients = new Array2DRowRealMatrix(coef, false);
        DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
        RealVector constants = new ArrayRealVector(results, false);
        double sol[] = solver.solve(constants).toArray();
        TransformData transData = new TransformData();
        transData.a = sol[0]; transData.b = sol[1];
        transData.A = sol[2]; transData.B = sol[3]; transData.C = sol[4];
        transData.D = sol[5]; transData.E = sol[6]; transData.F = sol[7];
        return transData;
    }

    public void applyCoordTransform() {
        /*  a * sx * dx + b * sy * dx - A * sx - B * sy - C = - c * dx
            a * sx * dy + b * sy * dy - D * sx - E * sy - F = - c * dy
            order of variables:   a, b, A, B, C, D, E, F
        * */
        transData = calculateTransformData(false);
        revTransData = calculateTransformData(true);
    }

    public interface FlushTransformHandler {
        void flushTransformHandler();
    }

    LinkedList <FlushTransformHandler> flushTransformHandlers = new LinkedList<>();

    public void addFlushTransformHandler(FlushTransformHandler handler) {
        flushTransformHandlers.add(handler);
    }

    public void flushTransform() {
        flushTransformHandlers.forEach(Draw.FlushTransformHandler::flushTransformHandler);
        corners = new Corner[] {
                new Corner(0, 0), new Corner(1, 0),
                new Corner(0, -1), new Corner(1, -1)
        };
        applyCoordTransform();
    }

    public void goToStrict(int x, int y) throws LaserConnectException {
        if (!useTransform) {
            laser.goToStrict(x * scale, -y * scale);
        }
        else {
            Double coord[] = transData.transXY((double) x / (double) image.W, -1d * (double) y / (double) image.H);
            laser.goToStrict(coord[0].intValue(), coord[1].intValue());
        }
    }

    public void goTo(int x, int y) throws LaserConnectException {
        if (!useTransform) {
            laser.goTo(x * scale, -y * scale);
        }
        else {
            Double coord[] = transData.transXY((double) x / (double) image.W, -1d * (double) y / (double) image.H);
            laser.goTo(coord[0].intValue(), coord[1].intValue());
        }
    }

    /* "Native" means that there is no scaling and y-inversing */
    public void goToNative(Double x, Double y) throws LaserConnectException {
        laser.goTo(x.intValue(), y.intValue());
    }

    public void goToNativeStrict(Double x, Double y) throws LaserConnectException {
        laser.goToStrict(x.intValue(), y.intValue());
    }


    public void flushLaser() {
        if (!useTransform) {
            laser.setAs00();
        }
    }


    public String getCoordStr() {
        Integer x = laser.getX(), y = laser.getY();
        String nativeCoord = "Pos: " + x + ", " + y;
        if (!useTransform)
            return nativeCoord;
        String transCoord = "TransPos: " + revTransData.transX(x, y) + ", " + revTransData.transY(x, y);
        return nativeCoord + "\n" + transCoord;
    }

    void initBeforeDraw(String fname) throws IOException {
        openImage(fname);
        if (fname == null)
            throw new IOException();
        flushLaser();
    }

    public void drawPicMagic1(String fname) throws IOException, LaserConnectException {
        initBeforeDraw(fname);
        int H = image.H, W = image.W;
        int M = 0, cM = 0;
        matrix = new boolean[W + 2][H + 2];
        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) {
                matrix[j + 1][i + 1] = image.getPixelBinary(j, i);
                M += (matrix[j + 1][i + 1] ? 1 : 0);
            }
        }
        while (true) {
            int x = 0, y = 0;
            boolean flag = false;
            for (y = 1; y < H + 1; y++) {
                for (x = 1; x < W + 1; x++) {
                    if (matrix[x][y]) {
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    break;
                }
            }
            if (!flag) {
                break;
            }
            while (matrix[x][y]) {
                int dl = 0, dr = 0;
                while (matrix[x - dl][y]) {
                    dl++;
                }
                while (matrix[x + dr][y]) {
                    dr++;
                }
                dl--;
                dr--;
                int nx = 0;
                if (dl < dr) {
                    for (int i = x - dl; i <= x + dr; i++) {
                        goToStrict(i - 1, y - 1);
                        laser.makeDot();
                        matrix[i][y] = false;
                        cM++;
                        if (matrix[i][y + 1]) {
                            nx = i;
                        }
                    }
                } else {
                    for (int i = x + dr; i >= x - dl; i--) {
                        goToStrict(i - 1, y - 1);
                        laser.makeDot();
                        matrix[i][y] = false;
                        cM++;
                        if (matrix[i][y + 1]) {
                            nx = i;
                        }
                    }
                }
                x = nx;
                y++;
            }
            System.err.println(cM * 100 / M + "% is send");
        }
        matrix = null;
    }

    public void drawPic(String fname) throws IOException, LaserConnectException {
        initBeforeDraw(fname);
        for (int y = 0; y < image.H; y++) {
            goToStrict(0, y);
            for (int x = 0; x < image.W; x++) {
                if (image.getPixelBinary(x, y)) {
                    goTo(x, y);
                    laser.fireP(image.getPixelPercents(x, y));
                }
            }
        }
    }

    public void drawPicUpward(String fname) throws IOException, LaserConnectException {
        initBeforeDraw(fname);
        for (int y = image.H - 1; y >= 0; y--) {
            goToStrict(0, y);
            for (int x = 0; x < image.W; x++) {
                if (image.getPixelBinary(x, y)) {
                    goTo(x, y);
                    laser.fireP(image.getPixelPercents(x, y));
                }
            }
        }
    }
    
    public void showCorners(String fname) throws IOException, LaserConnectException {
        initBeforeDraw(fname);
        int H = image.H, W = image.W;
        class P {
            public int x, y;
            P(int xArg, int yArg) { x = xArg; y = yArg; }
        }
        for (P tup : new P[] {new P(0, 0), new P(W, 0), new P(W, H), new P(0, H)}) {
            goToStrict(tup.x, tup.y);
            laser.fire(5);
        }
    }

}