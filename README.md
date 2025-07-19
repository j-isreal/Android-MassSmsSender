# README

## Android-MassSmsSender

<b>WARNING:</b>  This software uses the native SMS functionality on your Android device to send multiple SMS messages which will originate from your device's mobile number.  Consideration must be taken as mass
sending of SMS messages from non-bulk registered numbers may result in your mobile number becoming network-blocked and/or reported for SPAM, which could result in loss of service and/or fines or penalties!  

The creators of this test tool assume NO RESPONSIBILITY OR LIABILITY.

<b>Use at your own risk!</b>


# Usage

Download the latest Release .apk file and install on your Android device.

(Requires Send_SMS and Read_Contacts permissions.)

## Sending Single SMS Message
1.  Pick a Contact
2.  Enter a message to send.
3.  Press the 'SEND SMS' button.

## Sending Multiple SMS Messages
1.  Create a CSV file (a simple text file with .csv file extension) that contains one 10-digit number per line, no spaces or dashes, and upload it to your device's Download folder.
2.  Use the Android-MassSmsSender app's 'IMPORT CSV' button to load the file and the numbers it contains.
3.  Enter the text of the SMS message you want to send in the ```Enter message here``` box.
4.  Press the 'SEND SMS' button.

### Optional Logging
If you wish to save the log of the sending session, click the appropriate button after all SMS messages have been sent and the log file and status for each message will be saved in your Downloads folder for future viewing.



## License

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

Copyright © 2025 Jacob "Isreal" Eiler and Isreal Consulting, LLC.

https://icllc.cc/dev-tool-android-masssmssender/
