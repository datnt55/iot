package vmio.com.blemultipleconnect.Utilities;

public class MahonyAHRS
{
    float sampleFreq = 0;                                                       // sample frequency in Hz
    volatile float twoKp = 0;                                                   // 2 * proportional gain (Kp)
    volatile float twoKi = 0;                                                   // 2 * integral gain (Ki)
    volatile float q0 = 1.0f, q1 = 0.0f, q2 = 0.0f, q3 = 0.0f;                  // quaternion of sensor frame relative to auxiliary frame
    volatile float integralFBx = 0.0f, integralFBy = 0.0f, integralFBz = 0.0f;	// integral error terms scaled by Ki

    public float[] GetQuaternion() { return new float[] {q0,  q1, q2, q3 }; }

    public MahonyAHRS(float sample_freq, float kq, float ki)
    {
        sampleFreq = sample_freq;
        twoKp = kq;
        twoKi = ki;
    }

    public void Update(float gx, float gy, float gz, float ax, float ay, float az, float mx, float my, float mz)
    {
        float recipNorm;
        float q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;
        float hx, hy, bx, bz;
        float halfvx, halfvy, halfvz, halfwx, halfwy, halfwz;
        float halfex, halfey, halfez;
        float qa, qb, qc;

        // Use IMU algorithm if magnetometer measurement invalid (avoids NaN in magnetometer normalization)
        if ((mx == 0.0f) && (my == 0.0f) && (mz == 0.0f))
        {
            Update(gx, gy, gz, ax, ay, az);
            return;
        }

        // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalization)
        if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f)))
        {
            // Normalize accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Normalize magnetometer measurement
            recipNorm = invSqrt(mx * mx + my * my + mz * mz);
            mx *= recipNorm;
            my *= recipNorm;
            mz *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            q0q0 = q0 * q0;
            q0q1 = q0 * q1;
            q0q2 = q0 * q2;
            q0q3 = q0 * q3;
            q1q1 = q1 * q1;
            q1q2 = q1 * q2;
            q1q3 = q1 * q3;
            q2q2 = q2 * q2;
            q2q3 = q2 * q3;
            q3q3 = q3 * q3;

            // Reference direction of Earth's magnetic field
            hx = 2.0f * (mx * (0.5f - q2q2 - q3q3) + my * (q1q2 - q0q3) + mz * (q1q3 + q0q2));
            hy = 2.0f * (mx * (q1q2 + q0q3) + my * (0.5f - q1q1 - q3q3) + mz * (q2q3 - q0q1));
            bx = (float)Math.sqrt(hx * hx + hy * hy);
            bz = 2.0f * (mx * (q1q3 - q0q2) + my * (q2q3 + q0q1) + mz * (0.5f - q1q1 - q2q2));

            // Estimated direction of gravity and magnetic field
            halfvx = q1q3 - q0q2;
            halfvy = q0q1 + q2q3;
            halfvz = q0q0 - 0.5f + q3q3;
            halfwx = bx * (0.5f - q2q2 - q3q3) + bz * (q1q3 - q0q2);
            halfwy = bx * (q1q2 - q0q3) + bz * (q0q1 + q2q3);
            halfwz = bx * (q0q2 + q1q3) + bz * (0.5f - q1q1 - q2q2);

            // Error is sum of cross product between estimated direction and measured direction of field vectors
            halfex = (ay * halfvz - az * halfvy) + (my * halfwz - mz * halfwy);
            halfey = (az * halfvx - ax * halfvz) + (mz * halfwx - mx * halfwz);
            halfez = (ax * halfvy - ay * halfvx) + (mx * halfwy - my * halfwx);

            // Compute and apply integral feedback if enabled
            if (twoKi > 0.0f)
            {
                integralFBx += twoKi * halfex * (1.0f / sampleFreq);    // integral error scaled by Ki
                integralFBy += twoKi * halfey * (1.0f / sampleFreq);
                integralFBz += twoKi * halfez * (1.0f / sampleFreq);
                gx += integralFBx;  // apply integral feedback
                gy += integralFBy;
                gz += integralFBz;
            }
            else
            {
                integralFBx = 0.0f; // prevent integral windup
                integralFBy = 0.0f;
                integralFBz = 0.0f;
            }

            // Apply proportional feedback
            gx += twoKp * halfex;
            gy += twoKp * halfey;
            gz += twoKp * halfez;
        }

        // Integrate rate of change of quaternion
        gx *= (0.5f * (1.0f / sampleFreq));     // pre-multiply common factors
        gy *= (0.5f * (1.0f / sampleFreq));
        gz *= (0.5f * (1.0f / sampleFreq));
        qa = q0;
        qb = q1;
        qc = q2;
        q0 += (-qb * gx - qc * gy - q3 * gz);
        q1 += (qa * gx + qc * gz - q3 * gy);
        q2 += (qa * gy - qb * gz + q3 * gx);
        q3 += (qa * gz + qb * gy - qc * gx);

        // Normalize quaternion
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;
    }

    public void Update(float gx, float gy, float gz, float ax, float ay, float az)
    {
        float recipNorm;
        float halfvx, halfvy, halfvz;
        float halfex, halfey, halfez;
        float qa, qb, qc;

        // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalization)
        if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f)))
        {

            // Normalize accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Estimated direction of gravity and vector perpendicular to magnetic flux
            halfvx = q1 * q3 - q0 * q2;
            halfvy = q0 * q1 + q2 * q3;
            halfvz = q0 * q0 - 0.5f + q3 * q3;

            // Error is sum of cross product between estimated and measured direction of gravity
            halfex = (ay * halfvz - az * halfvy);
            halfey = (az * halfvx - ax * halfvz);
            halfez = (ax * halfvy - ay * halfvx);

            // Compute and apply integral feedback if enabled
            if (twoKi > 0.0f)
            {
                integralFBx += twoKi * halfex * (1.0f / sampleFreq);    // integral error scaled by Ki
                integralFBy += twoKi * halfey * (1.0f / sampleFreq);
                integralFBz += twoKi * halfez * (1.0f / sampleFreq);
                gx += integralFBx;  // apply integral feedback
                gy += integralFBy;
                gz += integralFBz;
            }
            else
            {
                integralFBx = 0.0f; // prevent integral windup
                integralFBy = 0.0f;
                integralFBz = 0.0f;
            }

            // Apply proportional feedback
            gx += twoKp * halfex;
            gy += twoKp * halfey;
            gz += twoKp * halfez;
        }

        // Integrate rate of change of quaternion
        gx *= (0.5f * (1.0f / sampleFreq));     // pre-multiply common factors
        gy *= (0.5f * (1.0f / sampleFreq));
        gz *= (0.5f * (1.0f / sampleFreq));
        qa = q0;
        qb = q1;
        qc = q2;
        q0 += (-qb * gx - qc * gy - q3 * gz);
        q1 += (qa * gx + qc * gz - q3 * gy);
        q2 += (qa * gy - qb * gz + q3 * gx);
        q3 += (qa * gz + qb * gy - qc * gx);

        // Normalize quaternion
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;
    }

    float invSqrt(float x)
    {
        //float halfx = 0.5f * x;
        //float y = x;
        //long i = *(long*)&y;
        //i = 0x5f3759df - (i >> 1);
        //y = *(float*)&i;
        //y = y * (1.5f - (halfx * y * y));
        //return y;

        return 1f / (float)Math.sqrt(x);
    }

    // [20190124	VMio] Correct Mercator matrix
    //const Mat T_WEWe = (cv::Mat_<float>(3, 3) << 0, -1, 0, 1, 0, 0, 0, 0, 1);
    /// <summary>
    /// Converts quaternion to rotation matrix.
    /// Index order is row major. See http://en.wikipedia.org/wiki/Row-major_order
    /// Conversion: https://www.mathworks.com/help/robotics/ref/quaternion.rotmat.html
    /// </summary>

    public  double[][] GetRotationMatrix()
    {
        float R11 = 2 * q0 * q0 - 1 + 2 * q1 * q1;
        float R12 = 2 * (q1 * q2 + q0 * q3);
        float R13 = 2 * (q1 * q3 - q0 * q2);
        float R21 = 2 * (q1 * q2 - q0 * q3);
        float R22 = 2 * q0 * q0 - 1 + 2 * q2 * q2;
        float R23 = 2 * (q2 * q3 + q0 * q1);
        float R31 = 2 * (q1 * q3 + q0 * q2);
        float R32 = 2 * (q2 * q3 - q0 * q1);
        float R33 = 2 * q0 * q0 - 1 + 2 * q3 * q3;

        //// for frame rotation (world)
        //return Mat((cv::Mat_<float>(3, 3) <<
        //	R11, R12, R13,
        //	R21, R22, R23,
        //	R31, R32, R33));

        // for point rotation (imu)
        //        //return Mat((cv::Mat_<float>(3, 3) <<
        //        	R11, R21, R31,
        //        	R12, R22, R32,
        //        	R13, R23, R33));

        //    Mat rotWeS = Mat((cv::Mat_<float>(3, 3) <<
        //    R11, R21, R31,
        //            R12, R22, R32,
        //            R13, R23, R33));

        //double[][] rotWeS = {{R11, R21, R31},{R12, R22, R32},{R13, R23, R33}};
        double[][] rotWeS = {{R11, R12, R13},{R21, R22, R23},{R31, R32, R33}};

        // [20190124	VMio] Experiment of above rotation matrix is X is North, Y is West then need to rotate about Z for correct Mercator (X is East and Y is North)
        // return Sensor frame in Mercator frame coordinate (sensor frame X -> down, Y -> right, X -> back)
        return rotWeS;//return multipleMatrix(Define.T_WEWe, rotWeS);
    }

    /// <summary>
    /// Converts quaternion to yaw (Z), pitch (Y), roll (X) Euler angles (in radians).
    /// https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
    /// </summary>
    public double[] GetEulersAngle()
    {
        //double yaw = atan2(2 * (q2 * q3 - q0 * q1), 2 * q0 * q0 - 1 + 2 * q3 * q3);
        //double roll = -atan((2.0 * (q1 * q3 + q0 * q2)) / sqrt(1.0 - sqr((2.0 * q1 * q3 + 2.0 * q0 * q2))));
        //double pitch = atan2(2 * (q1 * q2 - q0 * q3), 2 * q0 * q0 - 1 + 2 * q1 * q1);

        // roll (x-axis rotation)
        double sinr_cosp = +2.0 * (q0 * q1 + q2 * q3);
        double cosr_cosp = +1.0 - 2.0 * (q1 * q1 + q2 * q2);
        double roll = Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = +2.0 * (q0 * q2 - q3 * q1);
        double pitch = 0;
        if (Math.abs(sinp) >= 1)
            pitch = Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = +2.0 * (q0 * q3 + q1 * q2);
        double cosy_cosp = +1.0 - 2.0 * (q2 * q2 + q3 * q3);
        double yaw = Math.atan2(siny_cosp, cosy_cosp);

        return new double[] {roll, pitch, yaw};    // corresponding to X -> Y -> Z
    }

    // Calculates rotation matrix given euler angles.
    //https://www.learnopencv.com/rotation-matrix-to-euler-angles/
    public double[][] EulerAnglesToRotationMatrix(double[] theta)
    {
//        // Calculate rotation about x axis
//        double[][] R_x = {
//                {1, 0, 0},
//                {0, Math.cos(theta[0]), -Math.sin(theta[0])},
//                {0, Math.sin(theta[0]), Math.cos(theta[0])}
//        };
//
//        // Calculate rotation about y axis
//        double[][] R_y = {
//                {Math.cos(theta[1]), 0, Math.sin(theta[1])},
//                {0, 1, 0},
//                {-Math.sin(theta[1]), 0, Math.cos(theta[1])}
//        };
//
//        // Calculate rotation about z axis
//        double[][] R_z = {
//                {Math.cos(theta[2]), -Math.sin(theta[2]),0},
//                {Math.sin(theta[2]), Math.cos(theta[2]),0},
//                {0, 0, 1}
//        };
//
//        // Combined rotation matrix
//        double[][] R = MathUtils.multipleMatrix( MathUtils.multipleMatrix( R_z , R_y) , R_x);
//        return new double[][]{{R[0][0],R[1][0],R[2][0]},{R[0][1],R[1][1],R[2][1]},{R[0][2],R[1][2],R[2][2]}}; // transformation of R, it is different to the original source code

        double sa=Math.sin(theta[0]);
        double ca = Math.cos(theta[0]);
        double sb = Math.sin(theta[1]);
        double cb = Math.cos(theta[1]);
        double sh = Math.sin(theta[2]);
        double ch = Math.cos(theta[2]);
        double[][] R ={
                {cb*ch,cb*sh,-cb},
                {sa*sb*ch-ca*sh,sa*sb*sh+ca*ch,sa*cb},
                {ca*sb*ch+sa*sh,ca*sb*sh-sa*ch,ca*cb}
//                {ch*ca,-ch*sa*cb +sh*sb,ch*sa*sb+sh*cb},
//                {sa,ca*cb,-ca*sb},
//                {-sh*ca,sh*sa*cb+ch*sb,-sh*sa*sb+ch*cb}
        };

        return R;
    }



    // Calculates rotation matrix to euler angles
    // The result is the same as MATLAB except the order
    // of the euler angles ( x and z are swapped ).
    // https://pdfs.semanticscholar.org/6681/37fa4b875d890f446e689eea1e334bcf6bf6.pdf

    public double[] rotationMatrixToEulerAngles(double[][] R)
    {
        R = new double[][]{{R[0][0],R[1][0],R[2][0]},{R[0][1],R[1][1],R[2][1]},{R[0][2],R[1][2],R[2][2]}};
        double sy = Math.sqrt(R[0][0] * R[0][0] +  R[1][0] * R[1][0]);

        boolean singular = sy < 1e-6; // If

        double x, y, z;
        if (!singular)
        {
//            x = Math.atan2(R[2][1] , R[2][2]);
//            y = Math.atan2(-R[2][0], sy);
//            z = Math.atan2(R[1][0], R[0][0]);

            x = Math.atan2(R[1][2] , R[2][2]);
            y = Math.atan2(-R[0][2], sy);
            double s1=Math.sin(x), c1= Math.cos(x);
            z= Math.atan2(s1*R[2][0]-c1*R[1][0],c1*R[1][1]-s1*R[2][1]);
            //z = Math.atan2(R[0][1], R[0][0]);
        }
        else
        {
            x = Math.atan2(-R[1][2], R[1][1]);
            y = Math.atan2(-R[2][0], sy);
            z = 0;
        }
        return new double[]{x, y, z};
    }

    boolean closeEnough(double a, double b, double epsilon) {
        return (epsilon > Math.abs(a - b));
    }
}