using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;

public class PluginInterface : MonoBehaviour
{
	public Text text;

	AndroidJavaObject jo;

	// Use this for initialization
	void Awake()
	{
		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		AndroidJavaObject activity = jc.GetStatic<AndroidJavaObject>("currentActivity");
		//Debug.Log("Got Activity");
		jo = new AndroidJavaObject("com.josh.wiimoteconnector.WiimoteDriver",activity);
		//Debug.Log("Instantiated WiimoteDriver");
	}

	// Update is called once per frame
	void Update()
	{
		string rawstring = jo.Call<string>("getStatus");
		if (rawstring.Length > 0)
		{
			//Debug.Log("Got " + rawstring + " from plugin");
			text.text += rawstring;
			if (rawstring.Contains("ERROR"))
			{
				string errstring = jo.Call<string>("getError");
				//Debug.Log("Got " + errstring + " from plugin");
				text.text += errstring;
			}
		}

	}

	public void Connect()
	{
		jo.Call("Connect");
	}
}