/**
 * Copyright (c) 2024 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package rpggods.data.deity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.data.favor.FavorRange;
import rpggods.data.perk.Affinity;
import rpggods.data.perk.AffinityType;
import rpggods.data.perk.Perk;
import rpggods.data.perk.action.PerkAction;
import rpggods.data.perk.condition.PerkCondition;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Centralizes all offerings, sacrifices, and perks
 * to reduce expensive searches and sorts after data has been loaded.
 */
@Immutable
public class DeityContainer {

    public static final DeityContainer EMPTY = DeityContainer.builder(new ResourceLocation("null")).build();

    /** The ResourceLocation ID **/
    public final ResourceLocation id;
    /** List of Altars **/
    public final List<ResourceLocation> altarList;
    /** Map of Item ID to Offering(s) **/
    public final Map<ResourceLocation, List<ResourceLocation>> offeringMap;
    /** Map of Entity ID to Sacrifice(s) **/
    public final Map<ResourceLocation, List<ResourceLocation>> sacrificeMap;
    /** Map of PerkCondition.Type to Perk(s). May contain multiple instances of the same Perk. **/
    public final Map<Codec<? extends PerkCondition>, List<ResourceLocation>> perkByConditionMap;
    /** Map of PerkData.Type to Perk(s) **/
    public final Map<Codec<? extends PerkAction>, List<ResourceLocation>> perkByTypeMap;
    /** List of all Perks **/
    public final List<ResourceLocation> perkList;

    //// CONSTRUCTOR ////

    private DeityContainer(RegistryAccess registryAccess, ResourceLocation id) {
        this.id = id;
        this.altarList = ImmutableList.copyOf(altarList);
        this.offeringMap = ImmutableMap.copyOf(offeringMap);
        this.sacrificeMap = ImmutableMap.copyOf(sacrificeMap);
        this.perkByConditionMap = Collections.unmodifiableMap(new EnumMap<>(perkByConditionMap));
        this.perkByTypeMap = Collections.unmodifiableMap(new EnumMap<>(perkByTypeMap));
        this.perkList = ImmutableList.copyOf(perkList);
    }

    /**
     * @param id the ID of the associated Deity
     */
    public static DeityContainer.Builder builder(final ResourceLocation id) {
        return new DeityContainer.Builder(id);
    }

    //// GETTERS ////

    public ResourceLocation getId() {
        return id;
    }

    public List<ResourceLocation> getAltarList() {
        return altarList;
    }

    public Map<ResourceLocation, List<ResourceLocation>> getOfferingMap() {
        return offeringMap;
    }

    public Map<ResourceLocation, List<ResourceLocation>> getSacrificeMap() {
        return sacrificeMap;
    }

    public Map<Codec<? extends PerkCondition>, List<ResourceLocation>> getPerkByConditionMap() {
        return perkByConditionMap;
    }

    public Map<PerkAction.Type, List<ResourceLocation>> getPerkByTypeMap() {
        return perkByTypeMap;
    }

    public List<ResourceLocation> getPerkList() {
        return perkList;
    }

    public Optional<Deity> getDeity() {
        return Optional.ofNullable(RPGGods.DEITY_MAP.get(this.id));
    }

    public static MutableComponent getName(final ResourceLocation id) {
        return Component.translatable(Altar.createTranslationKey(id));
    }

    @Override
    public String toString() {
        int offerings = 0;
        for(List<ResourceLocation> o : offeringMap.values()) {
            offerings += o.size();
        }
        int sacrifices = 0;
        for(List<ResourceLocation> s : sacrificeMap.values()) {
            sacrifices += s.size();
        }
        final StringBuilder sb = new StringBuilder("DeityHelper:");
        sb.append(" id[").append(id).append("]");
        sb.append(" altars[").append(altarList.size()).append("]");
        sb.append(" offerings[").append(offerings).append("]");
        sb.append(" sacrifices[").append(sacrifices).append("]");
        sb.append(" perks[").append(perkList.size()).append("]");
        return sb.toString();
    }

    //// REGISTRY ////

    private static final Map<ResourceLocation, DeityContainer> REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, DeityContainer> CLIENT_REGISTRY = new HashMap<>();

    /**
     * @param isClientSide true to use the client side registry, necessary for caching when using LAN servers
     * @return the {@link DeityContainer} registry
     */
    private static Map<ResourceLocation, DeityContainer> getRegistry(final boolean isClientSide) {
        if(isClientSide) {
            return CLIENT_REGISTRY;
        }
        return REGISTRY;
    }

    /**
     * @param registryAccess the registry access
     * @param id the {@link Deity} ID
     * @return the cached {@link DeityContainer}
     */
    public static DeityContainer getOrCreate(final RegistryAccess registryAccess, final ResourceLocation id) {
        // get existing entry
        final Map<ResourceLocation, DeityContainer> registry = getRegistry(EffectiveSide.get().isClient());
        final DeityContainer entry = registry.get(id);
        if(entry != null) {
            return entry;
        }
        // create new entry
        final DeityContainer container = new DeityContainer(registryAccess, id);
        registry.put(id, container);
        return container;
    }

    /**
     * Loads all values in the {@link Deity} registry and creates {@link DeityContainer}s for each one.
     * @param registryAccess the registry access
     */
    @ApiStatus.Internal
    public static void populate(final RegistryAccess registryAccess) {
        // load golem registry
        final Registry<Deity> registry = registryAccess.registryOrThrow(RGRegistry.Keys.DEITIES);
        // resolve golem containers when the server starts to avoid lag spikes later
        for(ResourceLocation id : registry.keySet()) {
            DeityContainer.getOrCreate(registryAccess, id);
        }
    }

    /**
     * Clears the {@link DeityContainer} registry
     */
    @ApiStatus.Internal
    public static void reset() {
        getRegistry(EffectiveSide.get().isClient()).clear();
    }

    //// BUILDER ////

    public static class Builder {
        private final ResourceLocation id;
        private final List<ResourceLocation> altarList = new ArrayList<>();
        private final Map<ResourceLocation, List<ResourceLocation>> offeringMap = new HashMap<>();
        private final Map<ResourceLocation, List<ResourceLocation>> sacrificeMap = new HashMap<>();
        private final Map<PerkCondition.Type, List<ResourceLocation>> perkByConditionMap = new EnumMap<>(PerkCondition.Type.class);
        private final Map<PerkAction.Type, List<ResourceLocation>> perkByTypeMap = new EnumMap<>(PerkAction.Type.class);
        private final List<ResourceLocation> perkList = new ArrayList<>();

        /**
         * @param id the ID of the associated Deity
         */
        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder addAltar(ResourceLocation id) {
            this.altarList.add(id);
            return this;
        }

        public Builder addOffering(ResourceLocation id, Offering offering) {
            if (offering.getFavor() == 0 && !offering.getFunction().isPresent() && !offering.getTrade().isPresent()) {
                return this;
            }
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(offering.getAccept().getItem());
            this.offeringMap.computeIfAbsent(itemId, r -> new ArrayList<>()).add(id);
            return this;
        }

        public Builder addSacrifice(ResourceLocation id, Sacrifice sacrifice) {
            if (sacrifice.getFavor() == 0 && !sacrifice.getFunction().isPresent()) {
                return this;
            }
            ResourceLocation entityId = sacrifice.getEntity();
            this.sacrificeMap.computeIfAbsent(entityId, r -> new ArrayList<>()).add(id);
            return this;
        }

        public Builder addPerk(ResourceLocation id, Perk perk) {
            if (FavorRange.EMPTY.equals(perk.getRange()) || perk.getActions().isEmpty()) {
                return this;
            }
            for (PerkAction action : perk.getActions()) {
                if (action.getType() == PerkAction.Type.UNLOCK) {
                    Deity deity = RPGGods.DEITY_MAP.getOrDefault(action.getId().orElse(Deity.EMPTY.getId()), Deity.EMPTY);
                    if (!deity.isEnabled()) {
                        RPGGods.LOGGER.info("Skipping perk with ID " + id + " because it unlocks a deity that is disabled.");
                        return this;
                    }
                }
            }
            this.perkList.add(id);
            for (PerkCondition condition : perk.getConditions()) {
                this.perkByConditionMap.computeIfAbsent(condition.getType(), r -> new ArrayList<>()).add(id);
            }
            for (PerkAction action : perk.getActions()) {
                PerkAction.Type type = action.getType();
                this.perkByTypeMap.computeIfAbsent(type, r -> new ArrayList<>()).add(id);
                action.getAffinity().ifPresent(affinity -> RPGGods.AFFINITY
                        .computeIfAbsent(affinity.getEntity(), entityId -> new EnumMap<>(AffinityType.class))
                        .computeIfAbsent(affinity.getType(), affinityType -> new ArrayList<>()).add(id));
            }
            return this;
        }

        public DeityContainer build() {
            return new DeityContainer(id, altarList, offeringMap, sacrificeMap, perkByConditionMap, perkByTypeMap, perkList);
        }
    }
}
