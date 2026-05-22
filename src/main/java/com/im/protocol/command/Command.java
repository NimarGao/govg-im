package com.im.protocol.command;

public interface Command {
    Byte LOGIN_REQUEST = 1;
    Byte LOGIN_RESPONSE = 2;
    Byte MESSAGE_REQUEST = 3;
    Byte MESSAGE_RESPONSE = 4;
    Byte HEARTBEAT_REQUEST = 5;
    Byte HEARTBEAT_RESPONSE = 6;
    
    // Group Commands
    Byte CREATE_GROUP_REQUEST = 7;
    Byte CREATE_GROUP_RESPONSE = 8;
    Byte JOIN_GROUP_REQUEST = 9;
    Byte JOIN_GROUP_RESPONSE = 10;
    Byte GROUP_MESSAGE_REQUEST = 11;
    Byte GROUP_MESSAGE_RESPONSE = 12;

    // Reliability & Sync
    Byte ACK_PACKET = 13;
    Byte SYNC_MSG_REQUEST = 14; // Pull offline messages
    Byte SYNC_MSG_RESPONSE = 15;

    // Advanced features: Read Receipts & Typing Status
    Byte READ_RECEIPT_REQUEST = 16;
    Byte READ_RECEIPT_RESPONSE = 17;
    Byte TYPING_REQUEST = 18;
    Byte TYPING_RESPONSE = 19;
}
