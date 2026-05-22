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

    // Recall & Edit
    Byte RECALL_REQUEST = 21;
    Byte RECALL_RESPONSE = 22;
    Byte EDIT_REQUEST = 26;
    Byte EDIT_RESPONSE = 27;

    // Group Read Receipts
    Byte GROUP_READ_RECEIPT_REQUEST = 28;
    Byte GROUP_READ_RECEIPT_RESPONSE = 29;

    // Emoji Reactions
    Byte MESSAGE_REACTION_REQUEST = 30;
    Byte MESSAGE_REACTION_RESPONSE = 31;

    // Voice/Video Call Signaling
    Byte VOICE_CALL_REQUEST = 40;
    Byte VOICE_CALL_RESPONSE = 41;

    // User Status & Subscription
    Byte USER_STATUS_REQUEST = 42;
    Byte USER_STATUS_RESPONSE = 43;

    // Content Audit / Security Config
    Byte AUDIT_CONFIG_REQUEST = 44;
    Byte AUDIT_CONFIG_RESPONSE = 45;

    // Desk Customer Service
    Byte CUSTOM_DESK_REQUEST = 46;
    Byte CUSTOM_DESK_RESPONSE = 47;

    // Multi Voice Call (TUIRoomKit)
    Byte MULTI_VOICE_CALL_REQUEST = 48;
    Byte MULTI_VOICE_CALL_RESPONSE = 49;

    // Friend / Relation Chain (TUIContact)
    Byte FRIEND_ADD_REQUEST = 50;
    Byte FRIEND_ADD_RESPONSE = 51;
    Byte RELATION_ACTION_REQUEST = 52;
    Byte RELATION_ACTION_RESPONSE = 53;
}
