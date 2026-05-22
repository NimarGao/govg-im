package com.im.protocol.codec;

import com.im.protocol.Packet;
import com.im.protocol.command.*;
import com.im.protocol.serializer.Serializer;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

public class PacketCodec {

    public static final int MAGIC_NUMBER = 0x12345678;
    public static final PacketCodec INSTANCE = new PacketCodec();
    private final Map<Byte, Class<? extends Packet>> packetTypeMap;
    private final Map<Byte, Serializer> serializerMap;

    private PacketCodec() {
        packetTypeMap = new HashMap<>();
        packetTypeMap.put(Command.LOGIN_REQUEST, LoginRequestPacket.class);
        packetTypeMap.put(Command.LOGIN_RESPONSE, LoginResponsePacket.class);
        packetTypeMap.put(Command.MESSAGE_REQUEST, MessageRequestPacket.class);
        packetTypeMap.put(Command.MESSAGE_RESPONSE, MessageResponsePacket.class);
        packetTypeMap.put(Command.CREATE_GROUP_REQUEST, CreateGroupRequestPacket.class);
        packetTypeMap.put(Command.CREATE_GROUP_RESPONSE, CreateGroupResponsePacket.class);
        packetTypeMap.put(Command.JOIN_GROUP_REQUEST, JoinGroupRequestPacket.class);
        packetTypeMap.put(Command.JOIN_GROUP_RESPONSE, JoinGroupResponsePacket.class);
        packetTypeMap.put(Command.GROUP_MESSAGE_REQUEST, GroupMessageRequestPacket.class);
        packetTypeMap.put(Command.GROUP_MESSAGE_RESPONSE, GroupMessageResponsePacket.class);
        packetTypeMap.put(Command.ACK_PACKET, AckPacket.class);
        packetTypeMap.put(Command.READ_RECEIPT_REQUEST, ReadReceiptRequestPacket.class);
        packetTypeMap.put(Command.READ_RECEIPT_RESPONSE, ReadReceiptResponsePacket.class);
        packetTypeMap.put(Command.TYPING_REQUEST, TypingRequestPacket.class);
        packetTypeMap.put(Command.TYPING_RESPONSE, TypingResponsePacket.class);

        // Recall & Edit
        packetTypeMap.put(Command.RECALL_REQUEST, RecallRequestPacket.class);
        packetTypeMap.put(Command.RECALL_RESPONSE, RecallResponsePacket.class);
        packetTypeMap.put(Command.EDIT_REQUEST, EditRequestPacket.class);
        packetTypeMap.put(Command.EDIT_RESPONSE, EditResponsePacket.class);

        // Group Read Receipts
        packetTypeMap.put(Command.GROUP_READ_RECEIPT_REQUEST, GroupReadReceiptRequestPacket.class);
        packetTypeMap.put(Command.GROUP_READ_RECEIPT_RESPONSE, GroupReadReceiptResponsePacket.class);

        // Emoji Reactions
        packetTypeMap.put(Command.MESSAGE_REACTION_REQUEST, MessageReactionRequestPacket.class);
        packetTypeMap.put(Command.MESSAGE_REACTION_RESPONSE, MessageReactionResponsePacket.class);

        // Voice/Video Call Signaling
        packetTypeMap.put(Command.VOICE_CALL_REQUEST, VoiceCallRequestPacket.class);
        packetTypeMap.put(Command.VOICE_CALL_RESPONSE, VoiceCallResponsePacket.class);

        // User Status
        packetTypeMap.put(Command.USER_STATUS_REQUEST, UserStatusRequestPacket.class);
        packetTypeMap.put(Command.USER_STATUS_RESPONSE, UserStatusResponsePacket.class);

        // Content Auditing
        packetTypeMap.put(Command.AUDIT_CONFIG_REQUEST, AuditConfigRequestPacket.class);
        packetTypeMap.put(Command.AUDIT_CONFIG_RESPONSE, AuditConfigResponsePacket.class);

        // Customer Desk Service
        packetTypeMap.put(Command.CUSTOM_DESK_REQUEST, DeskRequestPacket.class);
        packetTypeMap.put(Command.CUSTOM_DESK_RESPONSE, DeskResponsePacket.class);

        // Multi Voice Call (TUIRoomKit)
        packetTypeMap.put(Command.MULTI_VOICE_CALL_REQUEST, MultiVoiceCallRequestPacket.class);
        packetTypeMap.put(Command.MULTI_VOICE_CALL_RESPONSE, MultiVoiceCallResponsePacket.class);

        // Friend / Relation Chain (TUIContact)
        packetTypeMap.put(Command.FRIEND_ADD_REQUEST, FriendAddRequestPacket.class);
        packetTypeMap.put(Command.FRIEND_ADD_RESPONSE, FriendAddResponsePacket.class);
        packetTypeMap.put(Command.RELATION_ACTION_REQUEST, RelationActionRequestPacket.class);
        packetTypeMap.put(Command.RELATION_ACTION_RESPONSE, RelationActionResponsePacket.class);

        serializerMap = new HashMap<>();
        Serializer serializer = new Serializer.JSONSerializer();
        serializerMap.put(serializer.getSerializerAlgorithm(), serializer);
    }

    public void encode(ByteBuf byteBuf, Packet packet) {
        // 1. Serialize java object to byte array
        byte[] bytes = Serializer.DEFAULT.serialize(packet);

        // 2. Encoding process
        byteBuf.writeInt(MAGIC_NUMBER);
        byteBuf.writeByte(packet.getVersion());
        byteBuf.writeByte(Serializer.DEFAULT.getSerializerAlgorithm());
        byteBuf.writeByte(packet.getCommand());
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);
    }

    public Packet decode(ByteBuf byteBuf) {
        // Skip magic number
        byteBuf.skipBytes(4);

        // Skip version
        byteBuf.skipBytes(1);

        // Serializer algorithm
        byte serializeAlgorithm = byteBuf.readByte();

        // Command
        byte command = byteBuf.readByte();

        // Data length
        int length = byteBuf.readInt();

        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);

        Class<? extends Packet> requestType = getRequestType(command);
        Serializer serializer = getSerializer(serializeAlgorithm);

        if (requestType != null && serializer != null) {
            return serializer.deserialize(requestType, bytes);
        }

        return null;
    }

    private Serializer getSerializer(byte serializeAlgorithm) {
        return serializerMap.get(serializeAlgorithm);
    }

    private Class<? extends Packet> getRequestType(byte command) {
        return packetTypeMap.get(command);
    }
}
