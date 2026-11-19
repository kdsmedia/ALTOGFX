# Connecting your guitar to your phone

You do not need expensive or proprietary equipment to connect your guitar to your phone. Android is Linux underneath, and most standard audio devices work.

## Connect using adapters

The simplest way is to use adapters. A guitar jack is 1/4" TRS and a phone input is a 3.5 mm four pole socket, so you need two adapters:

1. A 1/4" TRS to 3.5 mm adapter
2. A two way 3.5 mm splitter for microphone and headphones

Connect the guitar jack to the 1/4" TRS to 3.5 mm adapter, then connect that adapter to the splitter. Connect headphones or speakers to the splitter and connect the splitter to the phone. Restart ALTOGFX so that it routes audio through the splitter automatically, or set the routing manually in Settings, Audio Devices.

## Connect using a USB audio interface

You can also connect a guitar, microphone or another instrument to your phone using a USB audio interface. Because Android is essentially Linux, most class compliant USB audio devices work. ALTOGFX picks an external audio interface automatically, and you can also choose input and output devices manually in Settings, Audio Devices.

To connect with an audio interface:

1. Connect the interface to your phone using an OTG to USB adapter, then restart ALTOGFX.
2. Connect the guitar to the interface using a 1/4" TRS cable.
3. If ALTOGFX does not select the interface automatically, choose it in Settings, Audio Devices.

## Tips for low latency

- Use a USB audio interface when possible. It usually gives lower latency than the built-in microphone path.
- Enable low latency mode in the app settings.
- Close other apps that use audio while you play.
