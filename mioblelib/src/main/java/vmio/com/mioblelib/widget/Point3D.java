/**************************************************************************************************
 Filename:       Point3D.java

 Copyright (c) 2013 - 2014 Texas Instruments Incorporated

 All rights reserved not granted herein.
 Limited License.

 Texas Instruments Incorporated grants a world-wide, royalty-free,
 non-exclusive license under copyrights and patents it now or hereafter
 owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
 this software subject to the terms herein.  With respect to the foregoing patent
 license, such license is granted  solely to the extent that any such patent is necessary
 to Utilize the software alone.  The patent license shall not apply to any combinations which
 include this software, other than combinations with devices manufactured by or for TI ('TI Devices').
 No hardware patent is licensed hereunder.

 Redistributions must preserve existing copyright notices and reproduce this license (including the
 above copyright notice and the disclaimer and (if applicable) source code license limitations below)
 in the documentation and/or other materials provided with the distribution

 Redistribution and use in binary form, without modification, are permitted provided that the following
 conditions are met:

 * No reverse engineering, decompilation, or disassembly of this software is permitted with respect to any
 software provided in binary form.
 * any redistribution and use are licensed by TI for use only with TI Devices.
 * Nothing shall obligate TI to provide you with source code for the software licensed and provided to you in object code.

 If software source code is provided to you, modification and redistribution of the source code are permitted
 provided that the following conditions are met:

 * any redistribution and use of the source code, including any resulting derivative works, are licensed by
 TI for use only with TI Devices.
 * any redistribution and use of any object code compiled from the source code and any resulting derivative
 works, are licensed by TI for use only with TI Devices.

 Neither the name of Texas Instruments Incorporated nor the names of its suppliers may be used to endorse or
 promote products derived from this software without specific prior written permission.

 DISCLAIMER.

 THIS SOFTWARE IS PROVIDED BY TI AND TI'S LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL TI AND TI'S LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
package vmio.com.mioblelib.widget;

import static vmio.com.mioblelib.widget.Point3D.DistType.*;

/**
 * Auto generated wrapper class for the data with 3 dimensions.
 */
public class Point3D {
    public double x, y, z;
    public  enum DistType { XY , XZ , YZ , XYZ  };

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setPoint (Point3D point){
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return x*Math.PI/180 + ";"+y*Math.PI/180+";"+z*Math.PI/180;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Point3D other = (Point3D) obj;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
            return false;
        if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
            return false;
        if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
            return false;
        return true;
    }

    /// <summary>
    /// Normalize vector
    /// </summary>
    public Point3D normalize()
    {
        double max = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
        return max < 1E-6 ? this : new Point3D(x / max, y / max, z / max);
    }

    /// <summary>
    /// Dot product of 2 vectors
    /// </summary>
    public double dot( Point3D v2)
    {
        return x * v2.x + y * v2.y + z * v2.z;
    }

    /// <summary>
    /// Project a point P onto a plane defined by normal unit vector n and crossing point P0
    /// </summary>
    /// <param name="P0">a point lie on this plane </param>
    /// <param name="n">normal unit vector</param>
    /// <param name="P">vector for project</param>
    /// <returns>projected vector of v on plane</returns>
    public Point3D projectOntoPlane(Point3D P0, Point3D n)
    {
        Point3D v = new Point3D(x - P0.x, y - P0.y, z - P0.z);
        double dist = n.dot(v);

        return new Point3D(x - dist * n.x, y - dist * n.y, z - dist * n.z);
    }



        /// <summary>
    ///  Calculate squared distance between 2 points
    /// </summary>
    /// <param name="p1">the first point</param>
    /// <param name="p2">the second point</param>
    /// <param name="type">Euclidean distance type </param>
    /// <returns></returns>
    public double sqrDist(Point3D p2, DistType type)
    {
        switch (type)
        {
            case XY:
                return (x - p2.x) * (x - p2.x) + (y - p2.y) * (y - p2.y);
            case XZ:
                return (x - p2.x) * (x - p2.x) + (z - p2.z) * (z - p2.z);
            case YZ:
                return (y - p2.y) * (y- p2.y) + (z - p2.z) * (z - p2.z);
            case XYZ:
            default:
                return (x - p2.x) * (x - p2.x) + (y - p2.y) * (y - p2.y) + (z - p2.z) * (z - p2.z);
        }
    }

    //[20181126 VMio] Add clone for fix buffer of Point3Dis assigned as value instead of reference
    public Point3D clone()
    {
        return new Point3D(this.x, this.y, this.z);
    }
}
