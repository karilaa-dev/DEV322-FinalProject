# Noise and Motion Anomaly Detector

## Team Name: Banana Ginger

## Team Members:
- Dylan Verallo
- Henadzi Kirykovich
- Kyryl Andreiev
- Breshna Naim

## Local configuration

Create or update `local.properties` in the project root. This file is ignored by Git.

```properties
GOOGLE_MAPS_API_KEY=your_google_maps_android_key

MONGODB_CONNECTION_STRING=your_mongodb_atlas_connection_string
ATLAS_DATABASE=bananaginger
ATLAS_ANOMALY_COLLECTION=anomalies
ATLAS_EARTHQUAKE_COLLECTION=earthquakes
```

The app connects to the Atlas cluster directly because Atlas App Services Data API
is blocked for this account. It never uploads the phone/manual lookup location or
user threshold values.
