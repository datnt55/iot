/**************************************************************************************************
 Filename:       SensorTagSimpleKeysProfile.java

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

import java.util.List;

import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.utilities.SensorTagGatt;


public class SensorTagBatteryProfile {
    protected BluetoothDevice mBTDevice;
    protected BluetoothGattService mBTService;
    protected BleDevice mBTLeService;
    protected BluetoothGattCharacteristic dataC;
    protected BluetoothGattCharacteristic configC;
    protected BluetoothGattCharacteristic periodC;
    public boolean isEnabled;
    private SimpleKeyClickListener listener;
    public boolean isConfigured;
    private byte value;
    public SensorTagBatteryProfile(Context con, BluetoothDevice device, BluetoothGattService service, BleDevice controller) {
        this.mBTDevice = device;
        this.mBTService = service;
        this.mBTLeService = controller;
        value = 0x0;
        List<BluetoothGattCharacteristic> characteristics = this.mBTService.getCharacteristics();

        for (BluetoothGattCharacteristic c : characteristics) {
            if (c.getUuid().toString().equals(SensorTagGatt.UUID_BATTERY_DATA.toString())) {
                this.dataC = c;
            }
        }


    }

    public static boolean isCorrectService(BluetoothGattService service) {
        if ((service.getUuid().toString().compareTo(SensorTagGatt.UUID_BATTERY_SERV.toString())) == 0) {
            return true;
        } else return false;
    }
    protected String getProfileName()
    {
        Class<?> enclosingClass = getClass().getEnclosingClass();
        if (enclosingClass != null) {
            return enclosingClass.getName();
        } else {
            return getClass().getName();
        }
    }
    public int configureService() {
        int lastRequestId = this.mBTLeService.queueRequestCharacteristicValue(this.dataC, getProfileName() + "-Config");
        this.isConfigured = true;
        return lastRequestId;
    }
    public int deConfigureService() {
        int lastRequestId = this.mBTLeService.queueSetNotificationForCharacteristic(this.dataC, false, getProfileName() + "-Deconfig");
        this.isConfigured = true;
        this.isConfigured = false;
        return lastRequestId;
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public void didUpdateValueForCharacteristic(BluetoothGattCharacteristic c) {
        if (c.equals(this.dataC)) {
            byte[] value = c.getValue();
            switch (value[0]) {
                case 0x1:
                    listener.onClick(true, false);
                    break;
                case 0x2:
                    listener.onClick(false, true);
                    break;
                case 0x3:
                    listener.onClick(true, true);
                    break;
                case 0x4:
                    listener.onClick(false, false);
                    break;
                case 0x5:
                    listener.onClick(true, false);
                    break;
                case 0x6:
                    listener.onClick(false, true);
                    break;
                case 0x7:
                    listener.onClick(true, true);
                    break;
                default:
                    listener.onClick(false, false);
                    break;
            }
        }
    }

    public interface SimpleKeyClickListener {
        void onClick(boolean left, boolean right);
    }

    public BluetoothDevice getDevice() { return mBTDevice; }
}
