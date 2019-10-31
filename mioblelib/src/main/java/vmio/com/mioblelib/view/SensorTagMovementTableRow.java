/**************************************************************************************************
 Filename:       SensorTagMovementTableRow.java

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
package vmio.com.mioblelib.view;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import vmio.com.mioblelib.widget.SparkLineView;


public class SensorTagMovementTableRow extends GenericCharacteristicTableRow {
	public final SparkLineView sl4,sl5,sl6;
	public final SparkLineView sl7,sl8,sl9;
	public final TextView gyroValue;
	public final TextView magValue;
	public final ImageView iconGyro, iconMag;
	public final RelativeLayout rowLayoutGyro,rowLayoutMag ;
	public SensorTagMovementTableRow(Context con) {
		super(con);

		this.sl1.autoScale = this.sl2.autoScale = this.sl3.autoScale = true;
		this.sl1.autoScaleBounceBack = this.sl2.autoScaleBounceBack = this.sl3.autoScaleBounceBack = false;
		this.sl2.setVisibility(View.VISIBLE);
		this.sl3.setVisibility(View.VISIBLE);
		this.sl2.setEnabled(true);
		this.sl3.setEnabled(true);
		this.sl2.setColor(255, 0, 150, 125);
		this.sl3.setColor(255, 0, 0, 0);
		this.rowLayoutGyro = new RelativeLayout(this.context){
			{
				setId(71);
			}
		};
		this.rowLayoutMag = new RelativeLayout(this.context){
			{
				setId(72);
			}
		};

		this.iconGyro = new ImageView(con) {
			{
				setId(1000);
				setPadding(30,30,30,30);
			}
		};

		this.iconMag = new ImageView(con) {
			{
				setId(1001);
				setPadding(30,30,30,30);
			}
		};

		//One Sparkline showing Gyroscope trends
		this.sl4 = new SparkLineView(con) {
			{
				setVisibility(View.VISIBLE);
				autoScale = true;
				autoScaleBounceBack = true;
				setId(6);
			}
		};
		this.sl5 = new SparkLineView(con) {
			{
				setVisibility(View.VISIBLE);
				autoScale = true;
				autoScaleBounceBack = true;
				setColor(255, 0, 150, 125);
				setId(7);
			}
		};
		this.sl6 = new SparkLineView(con) {
			{
				setVisibility(View.VISIBLE);
				autoScale = true;
				autoScaleBounceBack = true;
				setColor(255, 0, 0, 0);
				setId(8);
			}
		};
		//Three Sparkline showing Magnetometer trends
		this.sl7 = new SparkLineView(con) {
			{
				setVisibility(View.VISIBLE);
				autoScale = true;
				autoScaleBounceBack = true;
				setId(9);
			}
		};
		this.sl8 = new SparkLineView(con) {
			{
				setVisibility(View.VISIBLE);
				setColor(255, 0, 150, 125);
				autoScale = true;
				autoScaleBounceBack = true;
				setId(10);
			}
		};
		this.sl9 = new SparkLineView(con) {
			{
				setVisibility(View.VISIBLE);
				setColor(255, 0, 0, 0);
				autoScale = true;
				autoScaleBounceBack = true;
				setId(11);
			}
		};
		this.gyroValue = new TextView(con) {
			{
				setTextSize(TypedValue.COMPLEX_UNIT_PT, 8.0f);
				setTextAlignment(TEXT_ALIGNMENT_VIEW_END);
				setId(12);
				setVisibility(View.VISIBLE);
			}
		};
		this.magValue = new TextView(con) {
			{
				setTextSize(TypedValue.COMPLEX_UNIT_PT, 8.0f);
				setTextAlignment(TEXT_ALIGNMENT_VIEW_END);
				setId(13);
				setVisibility(View.VISIBLE);
			}
		};


		//Setup layout for all cell elements
		RelativeLayout.LayoutParams iconItemParams = new RelativeLayout.LayoutParams(
				iconSize,
				iconSize) {
			{
				addRule(RelativeLayout.CENTER_VERTICAL,
						RelativeLayout.TRUE);
				addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			}

		};
		iconGyro.setLayoutParams(iconItemParams);


		RelativeLayout.LayoutParams iconMagItemParams = new RelativeLayout.LayoutParams(
				iconSize,
				iconSize) {
			{
				addRule(RelativeLayout.CENTER_VERTICAL,
						RelativeLayout.TRUE);
				addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			}

		};
		iconMag.setLayoutParams(iconMagItemParams);
		RelativeLayout.LayoutParams tmpLayoutParams = new RelativeLayout.LayoutParams(
		        RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
        tmpLayoutParams.addRule(RelativeLayout.BELOW,
				this.sl3.getId());
        tmpLayoutParams.addRule(RelativeLayout.RIGHT_OF, iconGyro.getId());
		tmpLayoutParams.rightMargin = 90;
		gyroValue.setLayoutParams(tmpLayoutParams);

        tmpLayoutParams = new RelativeLayout.LayoutParams(
		        RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		tmpLayoutParams.setMargins(0, 10, 0, 20);
        tmpLayoutParams.addRule(RelativeLayout.BELOW,
				gyroValue.getId());
        tmpLayoutParams.addRule(RelativeLayout.RIGHT_OF, iconGyro.getId());
		this.sl4.setLayoutParams(tmpLayoutParams);
		this.sl5.setLayoutParams(tmpLayoutParams);
		this.sl6.setLayoutParams(tmpLayoutParams);

        tmpLayoutParams = new RelativeLayout.LayoutParams(
		        RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
        tmpLayoutParams.addRule(RelativeLayout.BELOW,
				this.sl6.getId());
        tmpLayoutParams.addRule(RelativeLayout.RIGHT_OF, iconMag.getId());
		tmpLayoutParams.rightMargin = 90;
		magValue.setLayoutParams(tmpLayoutParams);

        tmpLayoutParams = new RelativeLayout.LayoutParams(
		        RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		tmpLayoutParams.setMargins(0, 10, 0, 20);
        tmpLayoutParams.addRule(RelativeLayout.BELOW,
				magValue.getId());
		tmpLayoutParams.addRule(RelativeLayout.RIGHT_OF, iconMag.getId());
        //tmpLayoutParams.addRule(RelativeLayout.RIGHT_OF, icon.getId());
		this.sl7.setLayoutParams(tmpLayoutParams);
		this.sl8.setLayoutParams(tmpLayoutParams);
		this.sl9.setLayoutParams(tmpLayoutParams);



		RelativeLayout.LayoutParams WOSLayoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		WOSLayoutParams.setMargins(0, 10, 0, 20);
		WOSLayoutParams.addRule(RelativeLayout.BELOW,
				sl9.getId());
		WOSLayoutParams.addRule(RelativeLayout.RIGHT_OF, icon.getId());


		rowLayoutGyro.addView(iconGyro);
		rowLayoutMag.addView(iconMag);
		rowLayoutGyro.addView(gyroValue);
		rowLayoutGyro.addView(this.sl4);
		rowLayoutGyro.addView(this.sl5);
		rowLayoutGyro.addView(this.sl6);

		rowLayoutMag.addView(magValue);
		rowLayoutMag.addView(this.sl7);
		rowLayoutMag.addView(this.sl8);
		rowLayoutMag.addView(this.sl9);

		layoutRoot.addView(rowLayoutGyro);
		layoutRoot.addView(rowLayoutMag);
		
		
	}
	@Override
	public void onClick(View v) {
		this.config = !this.config;
		Log.d("onClick","Row ID" + v.getId());
		//Toast.makeText(this.context, "Found row with title : " + this.title.getText(), Toast.LENGTH_SHORT).show();
		Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
		fadeOut.setAnimationListener(this);
		fadeOut.setDuration(500);
		fadeOut.setStartOffset(0);
		Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
		fadeIn.setAnimationListener(this);
		fadeIn.setDuration(500);
		fadeIn.setStartOffset(250);
		if (this.config == true) {
			this.sl1.startAnimation(fadeOut);
			this.sl2.startAnimation(fadeOut);
			this.sl3.startAnimation(fadeOut);
			this.sl4.startAnimation(fadeOut);
			this.sl5.startAnimation(fadeOut);
			this.sl6.startAnimation(fadeOut);
			this.sl7.startAnimation(fadeOut);
			this.sl8.startAnimation(fadeOut);
			this.sl9.startAnimation(fadeOut);
			this.value.startAnimation(fadeOut);
			this.gyroValue.startAnimation(fadeOut);
			this.magValue.startAnimation(fadeOut);
			this.onOffLegend.startAnimation(fadeIn);
			this.onOff.startAnimation(fadeIn);
			this.periodLegend.startAnimation(fadeIn);
			this.periodBar.startAnimation(fadeIn);
		}
		else {
			this.sl1.startAnimation(fadeIn);
			this.sl1.startAnimation(fadeIn);
			this.sl2.startAnimation(fadeIn);
			this.sl3.startAnimation(fadeIn);
			this.sl4.startAnimation(fadeIn);
			this.sl5.startAnimation(fadeIn);
			this.sl6.startAnimation(fadeIn);
			this.sl7.startAnimation(fadeIn);
			this.sl8.startAnimation(fadeIn);
			this.sl9.startAnimation(fadeIn);
			this.value.startAnimation(fadeIn);
			this.gyroValue.startAnimation(fadeIn);
			this.magValue.startAnimation(fadeIn);
			this.onOffLegend.startAnimation(fadeOut);
			this.onOff.startAnimation(fadeOut);
			this.periodLegend.startAnimation(fadeOut);
			this.periodBar.startAnimation(fadeOut);
		}
		
		
	}
	@Override
	public void onAnimationStart (Animation animation) {
		
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (this.config == true) {
			this.sl1.setVisibility(View.INVISIBLE);
			this.sl2.setVisibility(View.INVISIBLE);
			this.sl3.setVisibility(View.INVISIBLE);
			this.sl4.setVisibility(View.INVISIBLE);
			this.sl5.setVisibility(View.INVISIBLE);
			this.sl6.setVisibility(View.INVISIBLE);
			this.sl7.setVisibility(View.INVISIBLE);
			this.sl8.setVisibility(View.INVISIBLE);
			this.sl9.setVisibility(View.INVISIBLE);
			this.onOff.setVisibility(View.VISIBLE);
			this.onOffLegend.setVisibility(View.VISIBLE);
			this.periodBar.setVisibility(View.VISIBLE);
			this.periodLegend.setVisibility(View.VISIBLE);
			this.gyroValue.setVisibility(View.INVISIBLE);
			this.magValue.setVisibility(View.INVISIBLE);
			this.value.setVisibility(View.INVISIBLE);
		}
		else {
			this.sl1.setVisibility(View.VISIBLE);
			this.sl2.setVisibility(View.VISIBLE);
			this.sl3.setVisibility(View.VISIBLE);
			this.sl4.setVisibility(View.VISIBLE);
			this.sl5.setVisibility(View.VISIBLE);
			this.sl6.setVisibility(View.VISIBLE);
			this.sl7.setVisibility(View.VISIBLE);
			this.sl8.setVisibility(View.VISIBLE);
			this.sl9.setVisibility(View.VISIBLE);
			this.gyroValue.setVisibility(View.VISIBLE);
			this.magValue.setVisibility(View.VISIBLE);
			this.value.setVisibility(View.VISIBLE);
			this.onOff.setVisibility(View.INVISIBLE);
			this.onOffLegend.setVisibility(View.INVISIBLE);
			this.periodBar.setVisibility(View.INVISIBLE);
			this.periodLegend.setVisibility(View.INVISIBLE);
		}
	}

	public void updatePeriod(int period){
		Log.e("GenericBluetoothProfile", "Period Stop");
		final Intent intent = new Intent(ACTION_PERIOD_UPDATE);
		intent.putExtra(EXTRA_SERVICE_UUID, this.uuidLabel.getText());
		intent.putExtra(EXTRA_PERIOD,period);
		this.context.sendBroadcast(intent);
	}
}
