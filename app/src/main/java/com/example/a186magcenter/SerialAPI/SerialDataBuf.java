package com.example.a186magcenter.SerialAPI;

import java.text.SimpleDateFormat;

public class SerialDataBuf {

		public byte[] mData;
		public String mTime;

		public SerialDataBuf(byte[] buffer, int size){
			mData = new byte[size];
			for (int i = 0; i < size; i++)
				mData[i]=buffer[i];
			SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");       
			mTime = sDateFormat.format(new java.util.Date());
		}
}