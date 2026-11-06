package com.innowise.userservice.mapper;

import com.innowise.userservice.model.dto.CardCreateDto;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.model.dto.CardUpdateDto;
import com.innowise.userservice.model.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentCardMapper {

    @Mapping(target = "user.id", source = "userId")
    PaymentCard toEntity(CardCreateDto dto);

    @Mapping(target = "userId", source = "user.id")
    CardResponseDto toResponse(PaymentCard entity);

    void updateEntity(CardUpdateDto dto, @MappingTarget PaymentCard entity);
}
