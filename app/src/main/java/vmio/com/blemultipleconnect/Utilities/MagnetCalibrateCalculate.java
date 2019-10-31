package vmio.com.blemultipleconnect.Utilities;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vmio.com.mioblelib.widget.Point3D;

/**
 * Created by DatNT on 1/31/2018.
 */

public class MagnetCalibrateCalculate {
    double minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
    double meanX = 0, meanY = 0, meanZ = 0;

    // Data for calibration
    ArrayList<Point3D> mCalibData = new ArrayList<Point3D>();

    // biases and scales are stored in a 2 dimensions array
    // [0,0], [0,1], [0,2]: bias  x/y/z
    // [1,0], [1,1], [1,2]: scale x/y/z
    // [20181122    VMio] Initialize to default no bias and no scale
    // double[][] mcalibFactors = new double[][] {{0.0, 0.0, 0.0}, {1.0, 1.0, 1.0}}; // new double[2][3]
    double[][] mcalibFactors = new double[][] {{0.0, 0.0, 0.0}, {1.0, 1.0, 1.0}, {1.0,0.0,0.0},{0.0,1.0,0.0},{0.0,0.0,1.0}}; // [20181221 dungtv] new double[2][3] + double[3][3] for Sphere matrix

    private Context mContext;
    private String address;

    public MagnetCalibrateCalculate(Context mContext, String address) {
        this.mContext = mContext;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /// <summary>
    /// Get median value of a sorted array
    /// </summary>
    private double getMedianOfSortedArr(double[] arr)
    {
        int N = arr.length;
        if (N % 2 != 0)
            return arr[N / 2];
        return (arr[N / 2 - 1] + arr[N / 2]);
    }

    /// <summary>
    /// Add a magnetic point to array data for calibration
    /// </summary>
    /// <param name="m">magnetic vector in 3D space</param>
    /// <returns>is neither enough for calibration or not</returns>
    public boolean addData(Point3D m, int maxData)
    {
        mCalibData.add(m);

        if (minX > m.x)
            minX = m.x;
        else if (maxX < m.x)
            maxX = m.x;

        if (minY > m.y)
            minY = m.y;
        else if (maxY < m.y)
            maxY = m.y;

        if (minZ > m.z)
            minZ = m.z;
        else if (maxZ < m.z)
            maxZ = m.z;

        ALog.e("Num", mCalibData.size()+"");
        ALog.e("wx/wy/wz", (maxX - minX)+"    "+(maxY - minY) +"  " +  (maxZ - minZ));

        // Not enough data points yet
        if (mCalibData.size() < maxData) // minimum number of points required for calibration
            return false;

        // Enough data points then check other conditions
        double wx = maxX - minX;
        double wy = maxY - minY;
        double wz = maxZ - minZ;

        // data range of each Plane is not enough
        if(wx < 20 || wy < 20 || wz < 20) // minimum range
            return  false;

        return true;
    }

    /// <summary>
    /// Calculate calibration factors
    /// </summary>
    public double[][] calibrate(double[][] SphereMatrix)
    {
        if(mCalibData.size() <= 0)
            return mcalibFactors;
        // bias
        //mcalibFactors[0][0] = (maxX + minX) / 2;
        //mcalibFactors[0][1] = (maxY + minY) / 2;
        //mcalibFactors[0][2] = (maxZ + minZ) / 2;
        // [20181122    VMio] Since Max, Min is not stable value then use average instead
        Point3D sum = new Point3D(0,0,0);
        for (Point3D magnet : mCalibData) {
            sum.x += magnet.x;
            sum.y += magnet.y;
            sum.z += magnet.z;
        }
        mcalibFactors[0][0] = sum.x / mCalibData.size();
        mcalibFactors[0][1] = sum.y / mCalibData.size();
        mcalibFactors[0][2] = sum.z / mCalibData.size();

        // scale
        //mcalibFactors[1][0] = (maxX - minX) / 2;
        //mcalibFactors[1][1] = (maxY - minY) / 2;
        //mcalibFactors[1][2] = (maxZ - minZ) / 2;

        // [20181122    VMio] Since Max, Min is not stable value then use deviation for scale instead
        sum = new Point3D(0,0,0);
        for (Point3D magnet : mCalibData) {
            sum.x += Math.abs(magnet.x - mcalibFactors[0][0]);
            sum.y += Math.abs(magnet.y - mcalibFactors[0][1]);
            sum.z += Math.abs(magnet.z - mcalibFactors[0][2]);
        }
        // Replace offset & scale to fast calibration matrix
        // deviation
        mcalibFactors[1][0] = sum.x / mCalibData.size();
        mcalibFactors[1][1] = sum.y / mCalibData.size();
        mcalibFactors[1][2] = sum.z / mCalibData.size();

        // normalize deviation fro scaless
        double avgS = (mcalibFactors[1][0] + mcalibFactors[1][1] + mcalibFactors[1][2]) / 3;
        mcalibFactors[1][0] = avgS / mcalibFactors[1][0];
        mcalibFactors[1][1] = avgS / mcalibFactors[1][1];
        mcalibFactors[1][2] = avgS / mcalibFactors[1][2];

        // [20181221 dungtv] Add Sphere matrix to factor array

//        if(SphereMatrix != null) {
////            mcalibFactors[0][0] = SphereMatrix[0][1];
////            mcalibFactors[0][1] = SphereMatrix[0][2];
////            mcalibFactors[0][2] = SphereMatrix[0][3];
////
////            mcalibFactors[1][0] = SphereMatrix[1][1];
////            mcalibFactors[1][1] = SphereMatrix[1][2];
////            mcalibFactors[1][2] = SphereMatrix[1][3];
//
//            mcalibFactors[2][0] = SphereMatrix[2][0];
//            mcalibFactors[2][1] = SphereMatrix[2][1];
//            mcalibFactors[2][2] = SphereMatrix[2][2];
//
//            mcalibFactors[3][0] = SphereMatrix[3][0];
//            mcalibFactors[3][1] = SphereMatrix[3][1];
//            mcalibFactors[3][2] = SphereMatrix[3][2];
//
//            mcalibFactors[4][0] = SphereMatrix[4][0];
//            mcalibFactors[4][1] = SphereMatrix[4][1];
//            mcalibFactors[4][2] = SphereMatrix[4][2];
//
////            // Reset scale factor
////            mcalibFactors[1][0] = 1.0;
////            mcalibFactors[1][1] = 1.0;
////            mcalibFactors[1][2] = 1.0;
//        }

        return mcalibFactors;
    }

    public void setFactorFromServer(double[][] SphereMatrix){
        if(SphereMatrix != null) {
            mcalibFactors[0][0] = SphereMatrix[0][0];
            mcalibFactors[0][1] = SphereMatrix[0][1];
            mcalibFactors[0][2] = SphereMatrix[0][2];

            mcalibFactors[1][0] = SphereMatrix[1][0];
            mcalibFactors[1][1] = SphereMatrix[1][1];
            mcalibFactors[1][2] = SphereMatrix[1][2];

            mcalibFactors[2][0] = SphereMatrix[2][0];
            mcalibFactors[2][1] = SphereMatrix[2][1];
            mcalibFactors[2][2] = SphereMatrix[2][2];

            mcalibFactors[3][0] = SphereMatrix[3][0];
            mcalibFactors[3][1] = SphereMatrix[3][1];
            mcalibFactors[3][2] = SphereMatrix[3][2];

            mcalibFactors[4][0] = SphereMatrix[4][0];
            mcalibFactors[4][1] = SphereMatrix[4][1];
            mcalibFactors[4][2] = SphereMatrix[4][2];
        }
    }

    public void setSphereMatrix(double[][] SphereMatrix){
        if(SphereMatrix != null) {

            mcalibFactors[2][0] = SphereMatrix[2][0];
            mcalibFactors[2][1] = SphereMatrix[2][1];
            mcalibFactors[2][2] = SphereMatrix[2][2];

            mcalibFactors[3][0] = SphereMatrix[3][0];
            mcalibFactors[3][1] = SphereMatrix[3][1];
            mcalibFactors[3][2] = SphereMatrix[3][2];

            mcalibFactors[4][0] = SphereMatrix[4][0];
            mcalibFactors[4][1] = SphereMatrix[4][1];
            mcalibFactors[4][2] = SphereMatrix[4][2];
        }
    }

    // Save calib factors to shared preference
    public void saveFactor(String address){
        SharePreference preference = new SharePreference(mContext);
        preference.saveCalibrateMagnet(address, mcalibFactors);
        if(mcalibFactors != null && mcalibFactors.length >= 2 && mcalibFactors[0].length >= 3 && mcalibFactors[1].length >= 3 && mcalibFactors[2].length >= 3 && mcalibFactors[3].length >= 3 && mcalibFactors[4].length >= 3) {
            CommonUtils.printLog("======= Magnet Calibration Saved =======");
            CommonUtils.printLog(String.format("Magnet Offset: %.03f, %.03f, %.03f", mcalibFactors[0][0], mcalibFactors[0][1], mcalibFactors[0][2]));
            CommonUtils.printLog(String.format("Magnet Scale : %.03f, %.03f, %.03f", mcalibFactors[1][0], mcalibFactors[1][1], mcalibFactors[1][2]));

            CommonUtils.printLog(String.format("Sphere Matrix:"));
            CommonUtils.printLog(String.format("[%.03f, %.03f, %.03f]", mcalibFactors[2][0], mcalibFactors[2][1], mcalibFactors[2][2]));
            CommonUtils.printLog(String.format("[%.03f, %.03f, %.03f]", mcalibFactors[3][0], mcalibFactors[3][1], mcalibFactors[3][2]));
            CommonUtils.printLog(String.format("[%.03f, %.03f, %.03f]", mcalibFactors[4][0], mcalibFactors[4][1], mcalibFactors[4][2]));
            CommonUtils.printLog("========================================");
        }
    }

    // Get calib factors from shared preference
    public boolean getFactor(String address){
        SharePreference preference = new SharePreference(mContext);
        mcalibFactors = preference.getCalibrateMagnet(address);
        if(mcalibFactors != null && mcalibFactors.length >= 2 && mcalibFactors[0].length >= 3 && mcalibFactors[1].length >= 3 && mcalibFactors[2].length >= 3 && mcalibFactors[3].length >= 3 && mcalibFactors[4].length >= 3) {
            CommonUtils.printLog("======= Magnet Calibration Load =======");
            CommonUtils.printLog(String.format("Magnet Offset: %.03f, %.03f, %.03f", mcalibFactors[0][0], mcalibFactors[0][1], mcalibFactors[0][2]));
            CommonUtils.printLog(String.format("Magnet Scale : %.03f, %.03f, %.03f", mcalibFactors[1][0], mcalibFactors[1][1], mcalibFactors[1][2]));

            CommonUtils.printLog(String.format("Sphere Matrix:"));
            CommonUtils.printLog(String.format("[%.03f, %.03f, %.03f]", mcalibFactors[2][0], mcalibFactors[2][1], mcalibFactors[2][2]));
            CommonUtils.printLog(String.format("[%.03f, %.03f, %.03f]", mcalibFactors[3][0], mcalibFactors[3][1], mcalibFactors[3][2]));
            CommonUtils.printLog(String.format("[%.03f, %.03f, %.03f]", mcalibFactors[4][0], mcalibFactors[4][1], mcalibFactors[4][2]));
            CommonUtils.printLog("========================================");
        } else {
            CommonUtils.printLog("====== Magnet NOT Calibrated yet ======");
            // [20181122    VMio] Initialize to default no bias and no scale
            //mcalibFactors = new double[][] {{0.0, 0.0, 0.0}, {1.0, 1.0, 1.0}};
            mcalibFactors = new double[][] {{0.0, 0.0, 0.0}, {1.0, 1.0, 1.0}, {1.0,0.0,0.0},{0.0,1.0,0.0},{0.0,0.0,1.0}}; // [20181221 dungtv] new double[2][3] + double[3][3] for Sphere matrix
        }
        return mcalibFactors == null ? false : true;
    }
    /// <summary>
    /// Estimate the North direction from a given magnetic vector
    /// </summary>
    /// <param name="g">gravity vector<param>
    /// <param name="m">mamagnetic vector<param>
    /// <returns>angle in radian</returns>
    public double estimateNorth(Point3D g, Point3D m)
    {
        // z plane determined by vector gravity g, n is unit vector and O(0,0,0) is crossing point
        Point3D n = new Point3D(-g.x, -g.y, -g.z).normalize();

        // project vector m onto z plane
        m = m.projectOntoPlane(new Point3D(0, 0, 0), n);

        // calibrate
        m.x = (m.x - mcalibFactors[0][0]) * mcalibFactors[1][0];
        m.y = (m.y - mcalibFactors[0][1]) * mcalibFactors[1][1];
        m.z = (m.z - mcalibFactors[0][2]) * mcalibFactors[1][2];

        // normalize to unit vector
        Point3D m_norm = m.normalize();

        return Math.atan2(m_norm.y, m_norm.x);
    }

    public Point3D correctMagnetic(Point3D m)
    {
        if(mcalibFactors == null || mcalibFactors.length <= 0 || mcalibFactors[0].length < 3 || mcalibFactors[1].length < 3)
            return m;
        double x = (m.x - mcalibFactors[0][0]) * mcalibFactors[1][0];
        double y = (m.y - mcalibFactors[0][1]) * mcalibFactors[1][1];
        double z = (m.z - mcalibFactors[0][2]) * mcalibFactors[1][2];

        // [20181221 dungtv] Compensate by Sphere matrix(In this case the scale factors were already reset to 1.0)
        if(mcalibFactors[2].length >= 3 && mcalibFactors[3].length >= 3 && mcalibFactors[4].length >= 3)
        {
            double x2 = mcalibFactors[2][0] * x + mcalibFactors[2][1] * y + mcalibFactors[2][2] * z;
            double y2 = mcalibFactors[3][0] * x + mcalibFactors[3][1] * y + mcalibFactors[3][2] * z;
            double z2 = mcalibFactors[4][0] * x + mcalibFactors[4][1] * y + mcalibFactors[4][2] * z;
            x = x2; y = y2; z = z2;
        }

        return  new Point3D(x,y,z);
    }

    // [20181122    VMio] Get calibrate factors of Bias
    public Point3D GetBias()
    {
        return new Point3D(mcalibFactors[0][0], mcalibFactors[0][1], mcalibFactors[0][2]);
    }

    // [20181122    VMio] Get calibrate factors of Bias
    public Point3D GetScales()
    {
        return new Point3D(mcalibFactors[1][0], mcalibFactors[1][1], mcalibFactors[1][2]);
    }

    public double[][] GetSphereMatrix()
    {
        return new double[][]{
                {mcalibFactors[2][0], mcalibFactors[2][1], mcalibFactors[2][2]},
                {mcalibFactors[3][0], mcalibFactors[3][1], mcalibFactors[3][2]},
                {mcalibFactors[4][0], mcalibFactors[4][1], mcalibFactors[4][2]}};
    }

    public String biasAndScaleToString(){
        return mcalibFactors[0][0] +","+ mcalibFactors[0][1] +","+ mcalibFactors[0][2] +","+mcalibFactors[1][0] +","+ mcalibFactors[1][1] +","+ mcalibFactors[1][2];
    }
}
