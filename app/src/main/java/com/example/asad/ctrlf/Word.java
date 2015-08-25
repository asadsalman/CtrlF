package com.example.asad.ctrlf;


import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Word{

	static public int rotation;
	SortedMap<String, Set<String>> wordMap = new TreeMap<>();

	Word(String jsonStr){
		try {
			JSONObject object = new JSONObject(jsonStr);
			JSONArray regions = object.getJSONArray("regions");
			rotation = object.getInt("textAngle");

			for (int i = 0; i < regions.length(); i++) {
				JSONArray lines = regions.getJSONObject(i).getJSONArray("lines");
				for (int j = 0; j < lines.length(); j++) {
					JSONArray words = lines.getJSONObject(j).getJSONArray("words");
					for (int k = 0; k < words.length(); k++) {
						String word = words.getJSONObject(k).getString("text").replaceAll("(?!\")\\p{Punct}", "").toLowerCase().trim();
						String boundingBox = words.getJSONObject(k).getString("boundingBox");

						if (word.length() == 0)
							continue;

						Set<String> boundingBoxSet = new HashSet<>();
						boundingBoxSet.add(boundingBox);

						if (wordMap.containsKey(word)) {
							Set<String> setty = wordMap.get(word);
							boundingBoxSet.addAll(setty);
						}

						wordMap.put(word, new HashSet<>(boundingBoxSet));
					}
				}
			}

		} catch (Exception e) {e.printStackTrace();}
	}
	protected Set<Rect> getBoundingBoxSet(String word) {
		Set<Rect> emptySet = new HashSet<>();
		if(wordMap.keySet().contains(word))
			return stringSetToRect(wordMap.get(word));
		else
			return emptySet;

	}
	private Set<Rect> stringSetToRect(Set<String> setOfStrings) {
		Set<Rect> setOfRect = new HashSet<>();
		for(String str:setOfStrings) {
			String[] explode = str.split(",");
			int l = Integer.parseInt(explode[0]) - 10;
			int t = Integer.parseInt(explode[1]) - 10;
			int r = Integer.parseInt(explode[2]) + l + 10 + 10;
			int b = Integer.parseInt(explode[3]) + t + 10 + 10;

			setOfRect.add(new Rect(l, t, r, b));
		}

		return setOfRect;
	}
	public static boolean hasWords(String json){
		try{
			JSONObject object = new JSONObject(json);
			JSONArray regions = object.getJSONArray("regions");

			return regions.length()>0;

		}catch (JSONException je){
			je.printStackTrace();
			return false;
		}
	}

}