package ru.livingcivilizations.entity;

public enum NpcRole {
    TRAVELER("traveler", "Путешественник"),
    CARAVAN_MERCHANT("caravan_merchant", "Караванный торговец"),
    VILLAGE_MERCHANT("village_merchant", "Деревенский торговец"),
    WORKER("worker", "Работяга"),
    GUARD("guard", "Стражник"),
    WARRIOR("warrior", "Воин"),
    LORD("lord", "Лорд"),
    KING("king", "Король"),
    BANDIT("bandit", "Разбойник");

    private final String id;
    private final String displayName;

    NpcRole(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
}
