package vmio.com.blemultipleconnect.listener;

/**
 * Created by DatNT on 7/19/2017.
 */

public interface ItemTouchHelperAdapter {

    void onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);


}