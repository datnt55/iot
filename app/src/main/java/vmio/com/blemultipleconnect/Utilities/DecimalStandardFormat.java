package vmio.com.blemultipleconnect.Utilities;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Created by DatNT on 2/26/2018.
 */

public class DecimalStandardFormat {

    private DecimalFormat decimalFormat;

    public DecimalStandardFormat(String pattern) {
        decimalFormat = new DecimalFormat(pattern);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(dfs);
    }

    public String format(double number) {
        return decimalFormat.format(number);
    }
}
