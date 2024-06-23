/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.repository;

import alfio.model.AdditionalService;
import alfio.model.AdditionalServiceItem;
import alfio.model.AdditionalServiceItem.AdditionalServiceItemStatus;
import ch.digitalfondue.npjt.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@QueryRepository
public interface AdditionalServiceRepository {

    @Query("select * from additional_service_with_currency where event_id_fk = :eventId order by ordinal")
    List<AdditionalService> loadAllForEvent(@Bind("eventId") int eventId);

    NamedParameterJdbcTemplate getJdbcTemplate();

    default Map<Integer, Map<AdditionalServiceItemStatus, Integer>> getCount(int eventId) {
        Map<Integer, Map<AdditionalServiceItemStatus, Integer>> res = new HashMap<>();
        getJdbcTemplate().query("select count(*) as cnt, additional_service_id_fk, status from additional_service_item where event_id_fk = :eventId group by additional_service_id_fk, status",
            Collections.singletonMap("eventId", eventId),
            rse -> {
                var additionalServiceId = rse.getInt("additional_service_id_fk");
                res.putIfAbsent(additionalServiceId, new EnumMap<>(AdditionalServiceItem.AdditionalServiceItemStatus.class));
                var statusCount = res.get(additionalServiceId);
                var status = AdditionalServiceItem.AdditionalServiceItemStatus.valueOf(rse.getString("status"));
                statusCount.put(status, rse.getInt("cnt"));
            }
        );
        return res;
    }

    @Query("select * from additional_service_with_currency where id = :id and event_id_fk = :eventId")
    AdditionalService getById(@Bind("id") int id, @Bind("eventId") int eventId);

    @Query("select * from additional_service_with_currency where id = :id and event_id_fk = :eventId")
    Optional<AdditionalService> getOptionalById(@Bind("id") int id, @Bind("eventId") int eventId);

    @Query("delete from additional_service where id = :id and event_id_fk = :eventId")
    int delete(@Bind("id") int id, @Bind("eventId") int eventId);

    @Query("""
        insert into additional_service (event_id_fk, fix_price, ordinal, available_qty, max_qty_per_order, inception_ts, expiration_ts, vat, vat_type, price_cts, src_price_cts, service_type, supplement_policy, price_min_cts, price_max_cts) \
        values(:eventId, :fixPrice, :ordinal, :availableQty, :maxQtyPerOrder, :inceptionTs, :expirationTs, :vat, :vatType, 0, :srcPriceCts, :type, :supplementPolicy, :minPriceCts, :maxPriceCts)\
        """)
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Integer> insert(@Bind("eventId") int eventId, @Bind("srcPriceCts") int srcPriceCts, @Bind("fixPrice") boolean fixPrice,
                                           @Bind("ordinal") int ordinal, @Bind("availableQty") int availableQuantity, @Bind("maxQtyPerOrder") int maxQtyPerOrder,
                                           @Bind("inceptionTs") ZonedDateTime inception, @Bind("expirationTs") ZonedDateTime expiration, @Bind("vat") BigDecimal vat,
                                           @Bind("vatType") AdditionalService.VatType vatType,
                                           @Bind("type")AdditionalService.AdditionalServiceType type,
                                           @Bind("supplementPolicy") AdditionalService.SupplementPolicy supplementPolicy,
                                           @Bind("minPriceCts") Integer minPriceCts,
                                           @Bind("maxPriceCts") Integer maxPriceCts);

    @Query("""
        update additional_service set fix_price = :fixPrice, ordinal = :ordinal, available_qty = :availableQty, max_qty_per_order = :maxQtyPerOrder,\
         inception_ts = :inceptionTs, expiration_ts = :expirationTs, vat = :vat, vat_type = :vatType, src_price_cts = :srcPriceCts, supplement_policy = :policy where id = :id\
        """)
    int update(@Bind("id") int id, @Bind("fixPrice") boolean fixPrice,
               @Bind("ordinal") int ordinal, @Bind("availableQty") int availableQuantity, @Bind("maxQtyPerOrder") int maxQtyPerOrder,
               @Bind("inceptionTs") ZonedDateTime inception, @Bind("expirationTs") ZonedDateTime expiration, @Bind("vat") BigDecimal vat,
               @Bind("vatType") AdditionalService.VatType vatType, @Bind("srcPriceCts") int srcPriceCts, @Bind("policy") String policy);

    @Query("select * from additional_service_with_currency where event_id_fk = :eventId and supplement_policy = :supplementPolicy order by ordinal")
    List<AdditionalService> findAllInEventWithPolicy(@Bind("eventId") int eventId, @Bind("supplementPolicy") AdditionalService.SupplementPolicy policy);

    @Query("select * from additional_service_with_currency where event_id_fk = :eventId and id in (select additional_service_id_fk from additional_service_item where tickets_reservation_uuid = :reservationId) order by ordinal")
    List<AdditionalService> loadAllForReservation(@Bind("reservationId") String reservationId, @Bind("eventId") int eventId);

    @Query("select count(*) from additional_service where event_id_fk = :eventId and id in(:ids)")
    Integer countAdditionalServicesBelongingToEvent(@Bind("eventId") int eventId, @Bind("ids") Collection<Integer> ids);

    default Map<Integer, AdditionalService.AdditionalServiceType> getTypeByIds(Collection<Integer> additionalServiceIds) {
        Map<Integer, AdditionalService.AdditionalServiceType> res = new HashMap<>();
        getJdbcTemplate().query("select id, service_type from additional_service where id in (:additionalServiceIds)", Map.of("additionalServiceIds", additionalServiceIds), row -> {
            res.put(row.getInt("id"), AdditionalService.AdditionalServiceType.valueOf(row.getString("service_type")));
        });
        return res;
    }
}
