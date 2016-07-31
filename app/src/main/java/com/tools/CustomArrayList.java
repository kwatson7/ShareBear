package com.tools;

import java.util.ArrayList;

public class CustomArrayList <TYPE>
implements CustomList<TYPE>{

	private ArrayList<TYPE> data;
	private int currentIndex;

	public CustomArrayList(ArrayList<TYPE> data) {
		this.data = data;
		currentIndex = -1;
	}

	@Override
	public int size() {
		return data.size();
	}

	@Override
	public TYPE get(int index) {
		return data.get(index);
	}

	@Override
	public boolean moveToFirst() {
		if (size() < 1)
			return false;
		else{
			currentIndex = 0;
			return true;
		}
	}

	@Override
	public boolean moveToNext() {
		if (size() < 1 || currentIndex + 1 >= size())
			return false;
		else{
			currentIndex++;
			return true;
		}
	}

	@Override
	public int getPosition() {
		return currentIndex;
	}

	@Override
	public boolean moveToPosition(int index) {
		if (index < -1 || index >= size())
			return false;
		else{
			currentIndex = index;
			return true;
		}
	}

	@Override
	public boolean moveToLast() {
		if (size() < 1)
			return false;
		else{
			currentIndex = size()-1;
			return true;
		}
	}

	@Override
	public TYPE get() {
		return get(currentIndex);
	}

	@Override
	public void close() {
		data = null;	
	}

	@Override
	public void resample(int[] subsetIds) {
		if (subsetIds == null){
			data = new ArrayList<TYPE>(0);
			return;
		}
		ArrayList<TYPE> newList = new ArrayList<TYPE>(subsetIds.length);
		for (int i = 0; i < subsetIds.length; i++)
			newList.add(data.get(subsetIds[i]));
		data = newList;
	}
}
