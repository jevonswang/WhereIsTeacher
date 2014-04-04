package com.sutd.group3.roomfinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DisplayMessageActivity extends Activity {

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		initialize(savedInstanceState);

        Uri uri = getIntent().getData();
        Cursor cursor = managedQuery(uri, null, null, null, null);

        if (cursor == null) {
            finish();
        } else {
            cursor.moveToFirst();

            int wIndex = cursor.getColumnIndexOrThrow(DictionaryDatabase.KEY_WORD);
            int dIndex = cursor.getColumnIndexOrThrow(DictionaryDatabase.DATA);       
		
			String name = cursor.getString(wIndex);
			String data = cursor.getString(dIndex);
			
			
			Log.d("Tag", data);
			
			
			String[] strings = data.split(",");
			

			String room = strings[7] + strings[4];
			String email = strings[2];
			String phone = strings[3];
			
			
			//int floor = Integer.parseInt(strings[5].substring(1));
			int floor = Integer.parseInt(strings[5]);
			
			String sZone = strings[6];
			
			Zone zone = Zone.LIGHT_GREEN;
			/*
			if (sZone.equals("Light Green")){
				zone = Zone.LIGHT_GREEN;
			}
			else if (sZone.equals("Dark Green")){
				zone = Zone.DARK_GREEN;
			}
			else if (sZone.equals("Light Yellow")){
				zone = Zone.LIGHT_YELLOW;
			}
			else if (sZone.equals("Dark Yellow")){
				zone = Zone.DARK_YELLOW;
			}
			else if (sZone.equals("Light Orange")){
				zone = Zone.LIGHT_ORANGE;
			}
			else if (sZone.equals("Dark Orange")){
				zone = Zone.DARK_ORANGE;
			}
			else if (sZone.equals("Light Purple")){
				zone = Zone.LIGHT_PURPLE;
			}
			else if (sZone.equals("Dark Purple")){
				zone = Zone.DARK_PURPLE;
			}
			else if (sZone.equals("Light Pink")){
				zone = Zone.LIGHT_PINK;
			}
			else if (sZone.equals("Dark Pink")){
				zone = Zone.DARK_PINK;
			}
			else if (sZone.equals("Light Blue")){
				zone = Zone.LIGHT_BLUE;
			}
			else if (sZone.equals("Dark Blue")){
				zone = Zone.DARK_BLUE;
			}
			*/
			fillcontent(name, email, phone,floor, room, zone);
        }
	}

	private void initialize(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_display_message);
	}

	private void fillcontent(String name, String email,String phone,int floor, String room, Zone zone) {
		drawmap(floor);
		writetoast(floor);
		set_name(name);
		set_email(email);
		set_phone(phone);
		set_floor(floor);
		set_room(room);
		set_zone(zone);
	}

	private void drawmap(int floor) {
		ImageView imageView = (ImageView) findViewById(R.id.image);
		switch (floor) {
		case 2:
			imageView.setImageResource(R.drawable.floor_2_resized_alt);
			break;
		case 3:
			imageView.setImageResource(R.drawable.floor_3_resized_alt);
			break;
		case 4:
			imageView.setImageResource(R.drawable.floor_4_resized_alt);
			break;
		case 101:
			//Enyi
			imageView.setImageResource(R.drawable.enyi);
			break;
		case 102:
			//Bryan
			imageView.setImageResource(R.drawable.bryan);
			break;
		case 103:
			//Hon boon
			imageView.setImageResource(R.drawable.honboon);
			break;
		case 104:
			//Shuailong
			imageView.setImageResource(R.drawable.sl);
			break;
		case 105:
			//Kevin
			imageView.setImageResource(R.drawable.kevin);
			break;
		case 106:
			//Imma
			imageView.setImageResource(R.drawable.meng);
			break;
		default:
			imageView.setImageResource(R.drawable.meng);
			break;
		}
	}

	private void writetoast(int floor) {
		String toast_message = "双击地图放大";
		if (floor < 2 || floor > 4)
			toast_message = "详细信息没有找到，请升级数据库";
		if (floor >= 101 && floor <= 106)
			toast_message = "你发现了一个彩蛋,恭喜你！";
		final Toast toast = Toast.makeText(getApplicationContext(),
				toast_message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		new CountDownTimer(3000, 1000) {
			public void onTick(long millisUntilFinished) {
				toast.show();
			}

			public void onFinish() {
				toast.show();
			}
		}.start();
	}

	private void set_name(String name) {
		TextView text_name = (TextView) findViewById(R.id.text_name);
		text_name.setTextSize(20);
		if (name == "")
			name = "-";
		text_name.setText(Html.fromHtml("<b>姓名: </b>" + name));
	}
	
	private void set_email(String email) {
		TextView text_name = (TextView) findViewById(R.id.text_email);
		text_name.setTextSize(20);
		if (email == "")
			email = "-";
		text_name.setText(Html.fromHtml("<b>电子邮件: </b>" + email));
	}
	
	private void set_phone(String phone) {
		TextView text_name = (TextView) findViewById(R.id.text_phone);
		text_name.setTextSize(20);
		if (phone == "")
			phone = "-";
		text_name.setText(Html.fromHtml("<b>电话: </b>" + phone));
	}

	private void set_floor(int floor) {
		TextView text_floor = (TextView) findViewById(R.id.text_floor);
		text_floor.setTextSize(20);
		text_floor.setText(Html.fromHtml("<b>楼层: </b>" + floor));
	}

	private void set_room(String room) {
		if (room == "")
			room = "-";
		TextView text_room = (TextView) findViewById(R.id.text_room);
		text_room.setTextSize(20);
		text_room.setText(Html.fromHtml("<b>房间:</b> " + room));
	}

	private void set_zone(Zone zone) {
		String colour_hex = "";
		String colour_name = "";
		switch (zone) {
		case LIGHT_GREEN:
			colour_hex = "#69D4C3";
			colour_name = "Green (Light)";
			break;
		case DARK_GREEN:
			colour_hex = "#45D1BB";
			colour_name = "Green (Dark)";
			break;
		case LIGHT_YELLOW:
			colour_hex = "#F1F4A4";
			colour_name = "Yellow (Light)";
			break;
		case DARK_YELLOW:
			colour_hex = "#E8ED69";
			colour_name = "Yellow (Dark)";
			break;
		case LIGHT_ORANGE:
			colour_hex = "#FFCE8C";
			colour_name = "Orange (Light)";
			break;
		case DARK_ORANGE:
			colour_hex = "#FAA839";
			colour_name = "Orange (Dark)";
			break;
		case LIGHT_PURPLE:
			colour_hex = "#C1AEE9";
			colour_name = "Purple (Light)";
			break;
		case DARK_PURPLE:
			colour_hex = "#9A79DE";
			colour_name = "Purple (Dark)";
			break;
		case LIGHT_PINK:
			colour_hex = "#FFAAE7";
			colour_name = "Pink (Light)";
			break;
		case DARK_PINK:
			colour_hex = "#FE8CDE";
			colour_name = "Pink (Dark)";
			break;
		case LIGHT_BLUE:
			colour_hex = "#46B5CF";
			colour_name = "Blue (Light)";
			break;
		case DARK_BLUE:
			colour_hex = "#04A1C5";
			colour_name = "Blue (Dark)";
			break;
		default:
			colour_hex = "#000000";
			colour_name = "Unknown";
			break;
		}
		TextView text_section = (TextView) findViewById(R.id.text_section);
		text_section.setTextSize(20);
		text_section.setText(Html.fromHtml("<b>区域: </b><font color="
				+ colour_hex + ">" + colour_name + "</font>"));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}