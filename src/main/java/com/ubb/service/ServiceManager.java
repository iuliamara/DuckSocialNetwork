package com.ubb.service;

public class ServiceManager {
    private final UserServiceInterface userService;
    private final FriendshipServiceInterface friendshipService;
    private final NetworkServiceInterface networkService;
    private final MessageServiceInterface messageService;
    private final FriendshipRequestServiceInterface requestService;

    public ServiceManager(UserServiceInterface userService, FriendshipServiceInterface friendshipService, NetworkServiceInterface networkService, MessageServiceInterface messageService, FriendshipRequestServiceInterface requestService) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.networkService = networkService;
        this.messageService = messageService;
        this.requestService = requestService;
    }

    // Getters pentru toate
    public UserServiceInterface getUserService() { return userService; }
    public FriendshipServiceInterface getFriendshipService() { return friendshipService; }
    public NetworkServiceInterface getNetworkService() { return networkService; }
    public MessageServiceInterface getMessageService() { return messageService; }
    public FriendshipRequestServiceInterface getRequestService() { return requestService; }
}