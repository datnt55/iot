package vmio.com.blemultipleconnect.model;

import android.widget.Button;
import android.widget.ImageButton;

/**
 * Created by Duc on 12/7/2017.
 */

public class BleBody {

    private Button btnPosition;
    private String deviceAddress;

    public BleBody(Button btnPosition, String deviceAddress) {
        this.btnPosition = btnPosition;
        this.deviceAddress = deviceAddress;
    }

    public Button getBtnPosition() {
        return btnPosition;
    }

    public void setBtnPosition(Button btnPosition) {
        this.btnPosition = btnPosition;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
}
