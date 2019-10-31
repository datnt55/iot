package vmio.com.blemultipleconnect.model;

import android.widget.TextView;

/**
 * Created by DatNT on 11/2/2017.
 */

public class Title {
    private TextView title;
    private String address;

    public Title(TextView title, String address) {
        this.title = title;
        this.address = address;
    }

    public TextView getTitle() {
        return title;
    }

    public void setTitle(TextView title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
