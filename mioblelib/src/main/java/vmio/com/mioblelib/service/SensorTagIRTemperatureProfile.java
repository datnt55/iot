/**************************************************************************************************
 Filename:       SensorTagIRTemperatureProfile.java

 Copyright (c) 2013 - 2015 Texas Instruments Incorporated

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
package vmio.com.mioblelib.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vmio.com.mioblelib.R;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.view.GenericCharacteristicTableRow;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.mioblelib.widget.Sensor;


public class SensorTagIRTemperatureProfile extends GenericBluetoothProfile {
    public SensorTagIRTemperatureProfile(Context con, BluetoothDevice device, BluetoothGattService service, BleDevice controller) {
        super(con, device, service, controller);
        this.tRow = new GenericCharacteristicTableRow(con);

        List<BluetoothGattCharacteristic> characteristics = this.mBTService.getCharacteristics();

        for (BluetoothGattCharacteristic c : characteristics) {
            if (c.getUuid().toString().equals(SensorTagGatt.UUID_IRT_DATA.toString())) {
                this.dataC = c;
            }
            if (c.getUuid().toString().equals(SensorTagGatt.UUID_IRT_CONF.toString())) {
                this.configC = c;
            }
            if (c.getUuid().toString().equals(SensorTagGatt.UUID_IRT_PERI.toString())) {
                this.periodC = c;
            }
        }
        this.tRow.sl1.autoScale = true;
        this.tRow.sl1.autoScaleBounceBack = true;
        this.tRow.setIcon(this.getIconPrefix(), this.dataC.getUuid().toString());

        this.tRow.title.setText("非接触温度");
        this.tRow.uuidLabel.setText(this.dataC.getUuid().toString());
        this.tRow.value.setText("0.0'C");
        this.tRow.periodMinVal = 200;
        this.tRow.periodBar.setMax(255 - (this.tRow.periodMinVal / 10));
        this.tRow.periodBar.setProgress(100);
        this.tRow.icon.setImageResource(R.drawable.sensortag2irtemperature);
    }

    @Override
    public void didUpdateValueForCharacteristic(BluetoothGattCharacteristic c) {
        byte[] value = c.getValue();
        if (c.equals(this.dataC)) {
            if (this.mBTDevice == null){
                Point3D v = Sensor.IR_TEMPERATURE.convert(value);
                this.tRow.value.setText(String.format("%.1f°C", v.z));
                this.tRow.sl1.addValue((float) v.z);
            }else {
                if (this.mBTDevice.getName().equals("CC2650 SensorTag")) {
                    Point3D v = Sensor.IR_TEMPERATURE.convert(value);
                    this.tRow.value.setText(String.format("%.1f°C", v.z));
                    this.tRow.sl1.addValue((float) v.z);
                } else {
                    Point3D v = Sensor.IR_TEMPERATURE.convert(value);
                    if (this.tRow.config == false) {
                        this.tRow.value.setText(String.format("%.1f°C", v.y));
                    }
                    this.tRow.sl1.addValue((float) v.y);
                }
            }
        }
    }

    public static boolean isCorrectService(BluetoothGattService service) {
        if ((service.getUuid().toString().compareTo(SensorTagGatt.UUID_IRT_SERV.toString())) == 0) {
            return true;
        } else return false;
    }

    @Override
    public Map<String, String> getMQTTMap() {
        Point3D v = Sensor.IR_TEMPERATURE.convert(this.dataC.getValue());
        Map<String, String> map = new HashMap<String, String>();
        if (this.mBTDevice.getName().equals("CC2650 SensorTag")) {
            map.put("object_temp", String.format("%.2f", v.z));
        } else {
            map.put("object_temp", String.format("%.2f", v.y));
        }
        return map;
    }
}