# react-native-nfc-module
A NFC Android native module to integrate react-native application

# Only Manual Installation

# Step 1 : First, install the library from npm
```
npm install --save react-native-nfc-module
```

# Step 2, Link the native dependencies

### Mostly automatic installation

```
react-native link react-native-nfc-module
```

## Usage

1. import NFCScanner from 'react-native-nfc-module';

2. To check NFC availability 

    ```
    NFCScanner.NFCavailability( (resp) => {
            if(resp.nfcAvailable == "yes" || resp.nfcAvailable == "no") {
                if(resp.nfcAvailable == "yes")
                this.setState({nfcOpen: true});

                this.requestPermission();
            }
        });
    ```

3. Listening for NFC events
    
    step #1
    
    ```
    import {DeviceEventEmitter} from 'react-native';
    ```
    
    step #2
    
    ```
    componentDidMount() {
        /*
        Register listener
        */
        DeviceEventEmitter.addListener('onScanned', this.onScanned);
        
        /*
            onload check if there is NFC data 
            
            Caution : For multi window apps, there might be an issue if you dont handle the onResume & onStart events
        */
        NFCScanner.getInitialID("sample");
    }

    componentWillUnmount() {
        /*
        Unregister Listener
        */
        DeviceEventEmitter.removeListener('onScanned', this.onScanned);
    }
    ```

# Checklist
    
    Check #1 : Android Manifest.xml NFC permission
    
    ```
    <uses-permission android:name="android.permission.NFC" />
    ```
    
    Check #2 : Register What types of NFC should be handled
    
    ```
    <activity
            android:name=".MainActivity"
            ... >

            <intent-filter>
                <action android:name="android.nfc.action.TAG_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

            <intent-filter>
                <action android:name="android.nfc.action.NDEF_DISCOVERED"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <data android:mimeType="application/vnd.com.touchon.user"/>
            </intent-filter>

            <intent-filter>
                <action android:name="android.nfc.action.TECH_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

            <meta-data
                android:name="android.nfc.action.TECH_DISCOVERED"
                android:resource="@xml/nfc_tech_filter" />

        </activity>
        ```


# Issues

Check #1

this step is not necessary if the android manifest file is added with following dependencies

 check "react-native-nfc-module" project --> build.gradle

	add following
	    compile 'com.google.guava:guava:22.0-android'

	should look like

	dependencies {
	    compile 'com.facebook.react:react-native:+'
	    compile 'com.google.guava:guava:22.0-android'
	}

else do this

Open Android studio and open android project you will see "react-native-nfc-module" module 
        --> add "File dependency" in Module Settings --> choose "gauvalib.jar"
        
        