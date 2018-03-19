package com.adyen.mirakl.service;

import com.adyen.mirakl.domain.ShareholderMapping;
import com.adyen.mirakl.repository.ShareholderMappingRepository;
import com.adyen.model.Address;
import com.adyen.model.Name;
import com.adyen.model.marketpay.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.mirakl.client.mmp.domain.common.MiraklAdditionalFieldValue;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import com.mirakl.client.mmp.domain.shop.MiraklShops;
import com.mirakl.client.mmp.domain.shop.document.MiraklShopDocument;
import com.mirakl.client.mmp.operator.core.MiraklMarketplacePlatformOperatorApiClient;
import com.mirakl.client.mmp.request.shop.MiraklGetShopsRequest;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UboServiceTest {

    @InjectMocks
    private UboService uboService;

    private static final Map<String, String> UBO_FIELDS_ENUMS = ImmutableMap.of(
        "civility", "Mr",
        "phonetype", "MOBILE");

    private static final Set<String> UBO_FIELDS = ImmutableSet.of(
        "firstname",
        "lastname",
        "email",
        "dob",
        "nationality",
        "idnumber",
        "housenumber",
        "streetname",
        "city",
        "zip",
        "country",
        "phonecountry",
        "phonenumber");

    @Mock
    private MiraklShop miraklShopMock;
    @Mock
    private ShareholderMappingRepository shareholderMappingRepositoryMock;
    @Mock
    private ShareholderMapping shareholderMappingMock1, shareholderMappingMock2, shareholderMappingMock3, shareholderMappingMock4;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GetAccountHolderResponse existingAccountHolderMock;
    @Mock
    private ShareholderContact shareholderMock1, shareholderMock2, shareholderMock3, shareholderMock4;
    @Mock
    private MiraklShopDocument miraklShopDocument1, miraklShopDocument2, miraklShopDocument3;
    @Mock
    private MiraklMarketplacePlatformOperatorApiClient miraklMarketplacePlatformOperatorApiClientMock;
    @Mock
    private MiraklShops miraklShops1, miraklShops2;
    @Mock
    private MiraklShop miraklShop1, miraklShop2;
    @Mock
    private MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue miraklAddtionalField1, miraklAddtionalField2;

    @Captor
    private ArgumentCaptor<MiraklGetShopsRequest> miraklGetShopsRequestCaptor;



    @Test
    public void shouldCreateAllShareholdersFromUbos() {
        uboService.setMaxUbos(4);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", UBO_FIELDS, UBO_FIELDS_ENUMS);
        List<MiraklAdditionalFieldValue> ubo2 = createMiraklAdditionalUboField("2", UBO_FIELDS, UBO_FIELDS_ENUMS);
        List<MiraklAdditionalFieldValue> ubo3 = createMiraklAdditionalUboField("3", UBO_FIELDS, UBO_FIELDS_ENUMS);
        List<MiraklAdditionalFieldValue> ubo4 = createMiraklAdditionalUboField("4", UBO_FIELDS, UBO_FIELDS_ENUMS);
        final List<MiraklAdditionalFieldValue> additionalFields = Streams.concat(ubo1.stream(), ubo2.stream(), ubo3.stream(), ubo4.stream()).collect(Collectors.toList());
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(additionalFields);
        when(miraklShopMock.getId()).thenReturn("shopCode");
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 1)).thenReturn(Optional.of(shareholderMappingMock1));
        when(shareholderMappingMock1.getAdyenShareholderCode()).thenReturn("shareholderCode1");
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 2)).thenReturn(Optional.of(shareholderMappingMock2));
        when(shareholderMappingMock2.getAdyenShareholderCode()).thenReturn("shareholderCode2");
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 3)).thenReturn(Optional.of(shareholderMappingMock3));
        when(shareholderMappingMock3.getAdyenShareholderCode()).thenReturn("shareholderCode3");
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 4)).thenReturn(Optional.of(shareholderMappingMock4));
        when(shareholderMappingMock4.getAdyenShareholderCode()).thenReturn("shareholderCode4");

        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock);

        verifyShareHolders(result);
    }

    @Test
    public void shouldNotCreateIfMissingCivility() {
        uboService.setMaxUbos(1);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", ImmutableSet.of("firstname","lastname","email"), ImmutableMap.of());
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(ubo1);

        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotCreateIfMissingFirstName() {
        uboService.setMaxUbos(1);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", ImmutableSet.of("lastname","email"), ImmutableMap.of("civility", "Mr"));
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(ubo1);

        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotCreateIfMissingLastName() {
        uboService.setMaxUbos(1);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", ImmutableSet.of("firstname","email"), ImmutableMap.of("civility", "Mr"));
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(ubo1);

        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotCreateIfMissingEmail() {
        uboService.setMaxUbos(1);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", ImmutableSet.of("firstname","lastname"), ImmutableMap.of("civility", "Mr"));
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(ubo1);

        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock);

        Assertions.assertThat(result).isEmpty();
    }


    @Test
    public void shouldNotCreateDataIfMissing() {
        uboService.setMaxUbos(1);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", ImmutableSet.of("firstname","lastname","email"), ImmutableMap.of("civility", "Mr"));
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(ubo1);
        when(miraklShopMock.getId()).thenReturn("shopCode");
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 1)).thenReturn(Optional.empty());


        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock);

        Assertions.assertThat(result.size()).isOne();
        final ShareholderContact shareholderContact = result.iterator().next();
        Assertions.assertThat(shareholderContact.getName().getGender()).isEqualTo(Name.GenderEnum.MALE);
        Assertions.assertThat(shareholderContact.getName().getFirstName()).isEqualTo("firstname1");
        Assertions.assertThat(shareholderContact.getName().getLastName()).isEqualTo("lastname1");
        Assertions.assertThat(shareholderContact.getEmail()).isEqualTo("email1");
        Assertions.assertThat(shareholderContact.getPersonalData()).isNull();
        Assertions.assertThat(shareholderContact.getAddress()).isNull();
        Assertions.assertThat(shareholderContact.getPhoneNumber()).isNull();
        Assertions.assertThat(shareholderContact.getShareholderCode()).isNull();
    }

    @Test
    public void shouldUseMappingFromExistingShop(){
        uboService.setMaxUbos(4);
        List<MiraklAdditionalFieldValue> ubo1 = createMiraklAdditionalUboField("1", UBO_FIELDS, UBO_FIELDS_ENUMS);
        List<MiraklAdditionalFieldValue> ubo2 = createMiraklAdditionalUboField("2", UBO_FIELDS, UBO_FIELDS_ENUMS);
        List<MiraklAdditionalFieldValue> ubo3 = createMiraklAdditionalUboField("3", UBO_FIELDS, UBO_FIELDS_ENUMS);
        List<MiraklAdditionalFieldValue> ubo4 = createMiraklAdditionalUboField("4", UBO_FIELDS, UBO_FIELDS_ENUMS);
        final List<MiraklAdditionalFieldValue> additionalFields = Streams.concat(ubo1.stream(), ubo2.stream(), ubo3.stream(), ubo4.stream()).collect(Collectors.toList());
        when(miraklShopMock.getAdditionalFieldValues()).thenReturn(additionalFields);
        when(miraklShopMock.getId()).thenReturn("shopCode");
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 1)).thenReturn(Optional.empty());
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 2)).thenReturn(Optional.empty());
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 3)).thenReturn(Optional.empty());
        when(shareholderMappingRepositoryMock.findOneByMiraklShopIdAndMiraklUboNumber("shopCode", 4)).thenReturn(Optional.empty());

        when(existingAccountHolderMock.getAccountHolderDetails().getBusinessDetails().getShareholders()).thenReturn(ImmutableList.of(shareholderMock1, shareholderMock2, shareholderMock3, shareholderMock4));
        when(shareholderMock1.getShareholderCode()).thenReturn("shareholderCode1");
        when(shareholderMock2.getShareholderCode()).thenReturn("shareholderCode2");
        when(shareholderMock3.getShareholderCode()).thenReturn("shareholderCode3");
        when(shareholderMock4.getShareholderCode()).thenReturn("shareholderCode4");

        final List<ShareholderContact> result = uboService.extractUbos(miraklShopMock, existingAccountHolderMock);

        verifyShareHolders(result);
    }

    @Test
    public void shouldExtractMiraklDocumentsRelatedToUbos(){
        uboService.setMaxUbos(4);

        when(miraklShopDocument1.getTypeCode()).thenReturn("adyen-ubo1-photoid");
        when(miraklShopDocument1.getShopId()).thenReturn("shop1");
        when(miraklShopDocument2.getTypeCode()).thenReturn("adyen-ubo1-photoid-rear");
        when(miraklShopDocument2.getShopId()).thenReturn("shop1");
        when(miraklShopDocument3.getTypeCode()).thenReturn("adyen-ubo2-photoid");
        when(miraklShopDocument3.getShopId()).thenReturn("shop2");

        when(miraklMarketplacePlatformOperatorApiClientMock.getShops(miraklGetShopsRequestCaptor.capture())).thenReturn(miraklShops1).thenReturn(miraklShops2);
        when(miraklShops1.getShops()).thenReturn(ImmutableList.of(miraklShop1));
        when(miraklShops2.getShops()).thenReturn(ImmutableList.of(miraklShop2));
        when(miraklShop1.getAdditionalFieldValues()).thenReturn(ImmutableList.of(miraklAddtionalField1));
        when(miraklShop2.getAdditionalFieldValues()).thenReturn(ImmutableList.of(miraklAddtionalField2));
        when(miraklAddtionalField1.getCode()).thenReturn("adyen-ubo1-photoidtype");
        when(miraklAddtionalField1.getValue()).thenReturn("PASSPORT");
        when(miraklAddtionalField2.getCode()).thenReturn("adyen-ubo2-photoidtype");
        when(miraklAddtionalField2.getValue()).thenReturn("ID_CARD");

        Map<MiraklShopDocument, DocumentDetail.DocumentTypeEnum> result = uboService.extractUboDocuments(ImmutableList.of(miraklShopDocument1, miraklShopDocument2, miraklShopDocument3));

        List<MiraklGetShopsRequest> requestsToMirakl = miraklGetShopsRequestCaptor.getAllValues();
        Assertions.assertThat(requestsToMirakl.size()).isEqualTo(2);
        Assertions.assertThat(requestsToMirakl.get(0).getShopIds()).containsOnly("shop1");
        Assertions.assertThat(requestsToMirakl.get(1).getShopIds()).containsOnly("shop2");
        Assertions.assertThat(result.size()).isEqualTo(3);
        List<Map.Entry<MiraklShopDocument, DocumentDetail.DocumentTypeEnum>> results = new ArrayList<>(result.entrySet());
        Assertions.assertThat(results.get(0).getKey().getShopId()).isEqualTo("shop1");
        Assertions.assertThat(results.get(0).getKey().getTypeCode()).isEqualTo("adyen-ubo1-photoid");
        Assertions.assertThat(results.get(0).getValue()).isEqualTo(DocumentDetail.DocumentTypeEnum.PASSPORT);
        Assertions.assertThat(results.get(1).getKey().getShopId()).isEqualTo("shop1");
        Assertions.assertThat(results.get(1).getKey().getTypeCode()).isEqualTo("adyen-ubo1-photoid-rear");
        Assertions.assertThat(results.get(1).getValue()).isEqualTo(DocumentDetail.DocumentTypeEnum.PASSPORT);
        Assertions.assertThat(results.get(2).getKey().getShopId()).isEqualTo("shop2");
        Assertions.assertThat(results.get(2).getKey().getTypeCode()).isEqualTo("adyen-ubo2-photoid");
        Assertions.assertThat(results.get(2).getValue()).isEqualTo(DocumentDetail.DocumentTypeEnum.ID_CARD);
    }

    private void verifyShareHolders(final List<ShareholderContact> shareHolders) {
        final Set<Name.GenderEnum> genders = shareHolders.stream().map(ShareholderContact::getName).map(Name::getGender).collect(Collectors.toSet());
        Assertions.assertThat(genders).containsOnly(Name.GenderEnum.MALE);

        final Set<String> firstNames = shareHolders.stream().map(ShareholderContact::getName).map(Name::getFirstName).collect(Collectors.toSet());
        Assertions.assertThat(firstNames).containsExactlyInAnyOrder("firstname1", "firstname2", "firstname3", "firstname4");

        final Set<String> lastNames = shareHolders.stream().map(ShareholderContact::getName).map(Name::getLastName).collect(Collectors.toSet());
        Assertions.assertThat(lastNames).containsExactlyInAnyOrder("lastname1", "lastname2", "lastname3", "lastname4");

        final Set<String> emails = shareHolders.stream().map(ShareholderContact::getEmail).collect(Collectors.toSet());
        Assertions.assertThat(emails).containsExactlyInAnyOrder("email1", "email2", "email3", "email4");

        final Set<String> dateOfBirth = shareHolders.stream().map(ShareholderContact::getPersonalData).map(PersonalData::getDateOfBirth).collect(Collectors.toSet());
        Assertions.assertThat(dateOfBirth).containsExactlyInAnyOrder("dob1", "dob2", "dob3", "dob4");

        final Set<String> nationalities = shareHolders.stream().map(ShareholderContact::getPersonalData).map(PersonalData::getNationality).collect(Collectors.toSet());
        Assertions.assertThat(nationalities).containsExactlyInAnyOrder("nationality1", "nationality2", "nationality3", "nationality4");

        final Set<String> idNumbers = shareHolders.stream().map(ShareholderContact::getPersonalData).map(PersonalData::getIdNumber).collect(Collectors.toSet());
        Assertions.assertThat(idNumbers).containsExactlyInAnyOrder("idnumber1", "idnumber2", "idnumber3", "idnumber4");

        final Set<String> houseNumberOrName = shareHolders.stream().map(ShareholderContact::getAddress).map(Address::getHouseNumberOrName).collect(Collectors.toSet());
        Assertions.assertThat(houseNumberOrName).containsExactlyInAnyOrder("housenumber1", "housenumber2", "housenumber3", "housenumber4");

        final Set<String> streets = shareHolders.stream().map(ShareholderContact::getAddress).map(Address::getStreet).collect(Collectors.toSet());
        Assertions.assertThat(streets).containsExactlyInAnyOrder("streetname4", "streetname3", "streetname2", "streetname1");

        final Set<String> cities = shareHolders.stream().map(ShareholderContact::getAddress).map(Address::getCity).collect(Collectors.toSet());
        Assertions.assertThat(cities).containsExactlyInAnyOrder("city1", "city2", "city3", "city4");

        final Set<String> postalCodes = shareHolders.stream().map(ShareholderContact::getAddress).map(Address::getPostalCode).collect(Collectors.toSet());
        Assertions.assertThat(postalCodes).containsExactlyInAnyOrder("zip2", "zip1", "zip4", "zip3");

        final Set<String> countries = shareHolders.stream().map(ShareholderContact::getAddress).map(Address::getCountry).collect(Collectors.toSet());
        Assertions.assertThat(countries).containsExactlyInAnyOrder("country1", "country2", "country3", "country4");

        final Set<String> phoneCountries = shareHolders.stream().map(ShareholderContact::getPhoneNumber).map(PhoneNumber::getPhoneCountryCode).collect(Collectors.toSet());
        Assertions.assertThat(phoneCountries).containsExactlyInAnyOrder("phonecountry1", "phonecountry2", "phonecountry3", "phonecountry4");

        final Set<PhoneNumber.PhoneTypeEnum> phoneTypes = shareHolders.stream().map(ShareholderContact::getPhoneNumber).map(PhoneNumber::getPhoneType).collect(Collectors.toSet());
        Assertions.assertThat(phoneTypes).containsExactly(PhoneNumber.PhoneTypeEnum.MOBILE);

        final Set<String> phoneNumbers = shareHolders.stream().map(ShareholderContact::getPhoneNumber).map(PhoneNumber::getPhoneNumber).collect(Collectors.toSet());
        Assertions.assertThat(phoneNumbers).containsExactlyInAnyOrder("phonenumber1", "phonenumber2", "phonenumber3", "phonenumber4");

        final Set<String> shareholderCodes = shareHolders.stream().map(ShareholderContact::getShareholderCode).collect(Collectors.toSet());
        Assertions.assertThat(shareholderCodes).containsExactlyInAnyOrder("shareholderCode1", "shareholderCode2", "shareholderCode3", "shareholderCode4");
    }

    private List<MiraklAdditionalFieldValue> createMiraklAdditionalUboField(String uboNumber, Set<String> uboFields, Map<String, String> uboEnumFields) {
        final ImmutableList.Builder<MiraklAdditionalFieldValue> builder = new ImmutableList.Builder<>();
        uboFields.forEach(uboFieldName -> {
            MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue additionalField = new MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue();
            additionalField.setCode("adyen-ubo" + uboNumber + "-" + uboFieldName);
            additionalField.setValue(uboFieldName + uboNumber);
            builder.add(additionalField);
        });
        uboEnumFields.forEach((k, v) -> {
            MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue additionalField = new MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue();
            additionalField.setCode("adyen-ubo" + uboNumber + "-" + k);
            additionalField.setValue(v);
            builder.add(additionalField);
        });
        return builder.build();
    }

}
