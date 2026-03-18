package de.flori.exotico.data;

import java.util.List;

public class ExoticResponse {
    public List<Profile> profiles;
    public List<ItemObject> exoticItemsCombined;
    public int totalItems;
    public boolean cached;
    public long cacheAge;

    public static class Profile {
        public String name;
        public List<ItemObject> items;
        public List<ItemObject> exoticItems;
        public List<ItemObject> specialItems;
    }
}