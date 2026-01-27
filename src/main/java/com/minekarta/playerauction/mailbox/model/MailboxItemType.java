package com.minekarta.playerauction.mailbox.model;

/**
 * Types of items that can be in a mailbox
 */
public enum MailboxItemType {
    /**
     * Physical item from expired/cancelled auction
     */
    ITEM,

    /**
     * Money from sold auction
     */
    MONEY
}
