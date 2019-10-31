package vmio.com.blemultipleconnect.model;

import android.widget.TableLayout;

/**
 * Created by DatNT on 9/22/2017.
 */

public class TableView {
    private boolean first;
    private TableLayout row;

    public TableView(TableLayout row) {
        this.row = row;
        this.first = true;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public TableLayout getRow() {
        return row;
    }

    public void setRow(TableLayout row) {
        this.row = row;
    }
}
