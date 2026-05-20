-- V150: Seed all India states/UTs and their districts (LGD-aligned, 2024 data).
--
-- 28 States + 8 Union Territories, ~780 districts total.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. ANDHRA PRADESH (AP)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Andhra Pradesh', 'AP');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Alluri Sitharama Raju','Anakapalli','Ananthapuramu','Bapatla','Chittoor',
               'Dr. B.R. Ambedkar Konaseema','East Godavari','Eluru','Guntur','Kakinada',
               'Krishna','Kurnool','Nandyal','Nellore','NTR','Palnadu','Parvathipuram Manyam',
               'Prakasam','Srikakulam','Sri Potti Sriramulu Nellore','Sri Sathya Sai',
               'Tirupati','Visakhapatnam','Vizianagaram','West Godavari','YSR Kadapa']) AS t(d)
WHERE name = 'Andhra Pradesh';

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. ARUNACHAL PRADESH (AR)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Arunachal Pradesh', 'AR');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Anjaw','Changlang','Dibang Valley','East Kameng','East Siang','Itanagar Capital Complex',
               'Kamle','Kra Daadi','Kurung Kumey','Lepa Rada','Lohit','Longding','Lower Dibang Valley',
               'Lower Siang','Lower Subansiri','Namsai','Pakke Kessang','Papum Pare','Shi Yomi',
               'Siang','Tawang','Tirap','Upper Siang','Upper Subansiri','West Kameng','West Siang']) AS t(d)
WHERE name = 'Arunachal Pradesh';

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. ASSAM (AS)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Assam', 'AS');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Bajali','Baksa','Barpeta','Biswanath','Bongaigaon','Cachar','Charaideo',
               'Chirang','Darrang','Dhemaji','Dhubri','Dibrugarh','Dima Hasao','Goalpara',
               'Golaghat','Hailakandi','Hojai','Jorhat','Kamrup','Kamrup Metropolitan',
               'Karbi Anglong','Karimganj','Kokrajhar','Lakhimpur','Majuli','Morigaon',
               'Nagaon','Nalbari','Sivasagar','Sonitpur','South Salmara-Mankachar',
               'Tamulpur','Tinsukia','Udalguri','West Karbi Anglong']) AS t(d)
WHERE name = 'Assam';

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. BIHAR (BR)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Bihar', 'BR');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Araria','Arwal','Aurangabad','Banka','Begusarai','Bhagalpur','Bhojpur',
               'Buxar','Darbhanga','East Champaran','Gaya','Gopalganj','Jamui','Jehanabad',
               'Kaimur','Katihar','Khagaria','Kishanganj','Lakhisarai','Madhepura','Madhubani',
               'Munger','Muzaffarpur','Nalanda','Nawada','Patna','Purnia','Rohtas','Saharsa',
               'Samastipur','Saran','Sheikhpura','Sheohar','Sitamarhi','Siwan','Supaul',
               'Vaishali','West Champaran']) AS t(d)
WHERE name = 'Bihar';

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. CHHATTISGARH (CG)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Chhattisgarh', 'CG');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Balod','Baloda Bazar','Balrampur','Bastar','Bemetara','Bijapur','Bilaspur',
               'Dantewada','Dhamtari','Durg','Gariaband','Gaurela-Pendra-Marwahi','Janjgir-Champa',
               'Jashpur','Kabirdham','Kanker','Kondagaon','Korba','Koriya','Mahasamund',
               'Manendragarh-Chirmiri-Bharatpur','Mohla-Manpur-Ambagarh Chowki','Mungeli',
               'Narayanpur','Raigarh','Raipur','Rajnandgaon','Sakti','Sarangarh-Bilaigarh',
               'Sukma','Surajpur','Surguja']) AS t(d)
WHERE name = 'Chhattisgarh';

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. GOA (GA)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Goa', 'GA');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['North Goa','South Goa']) AS t(d)
WHERE name = 'Goa';

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. GUJARAT (GJ)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Gujarat', 'GJ');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Ahmedabad','Amreli','Anand','Aravalli','Banaskantha','Bharuch','Bhavnagar',
               'Botad','Chhota Udaipur','Dahod','Dang','Devbhoomi Dwarka','Gandhinagar',
               'Gir Somnath','Jamnagar','Junagadh','Kheda','Kutch','Mahisagar','Mehsana',
               'Morbi','Narmada','Navsari','Panchmahal','Patan','Porbandar','Rajkot',
               'Sabarkantha','Surat','Surendranagar','Tapi','Vadodara','Valsad']) AS t(d)
WHERE name = 'Gujarat';

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. HARYANA (HR)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Haryana', 'HR');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Ambala','Bhiwani','Charkhi Dadri','Faridabad','Fatehabad','Gurugram','Hisar',
               'Jhajjar','Jind','Kaithal','Karnal','Kurukshetra','Mahendragarh','Nuh',
               'Palwal','Panchkula','Panipat','Rewari','Rohtak','Sirsa','Sonipat','Yamunanagar']) AS t(d)
WHERE name = 'Haryana';

-- ─────────────────────────────────────────────────────────────────────────────
-- 9. HIMACHAL PRADESH (HP)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Himachal Pradesh', 'HP');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Bilaspur','Chamba','Hamirpur','Kangra','Kinnaur','Kullu','Lahaul and Spiti',
               'Mandi','Shimla','Sirmaur','Solan','Una']) AS t(d)
WHERE name = 'Himachal Pradesh';

-- ─────────────────────────────────────────────────────────────────────────────
-- 10. JHARKHAND (JH)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Jharkhand', 'JH');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Bokaro','Chatra','Deoghar','Dhanbad','Dumka','East Singhbhum','Garhwa',
               'Giridih','Godda','Gumla','Hazaribagh','Jamtara','Khunti','Koderma',
               'Latehar','Lohardaga','Pakur','Palamu','Ramgarh','Ranchi','Sahibganj',
               'Seraikela Kharsawan','Simdega','West Singhbhum']) AS t(d)
WHERE name = 'Jharkhand';

-- ─────────────────────────────────────────────────────────────────────────────
-- 11. KARNATAKA (KA)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Karnataka', 'KA');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Bagalkote','Ballari','Belagavi','Bengaluru Rural','Bengaluru Urban','Bidar',
               'Chamarajanagara','Chikkaballapura','Chikkamagaluru','Chitradurga','Dakshina Kannada',
               'Davanagere','Dharwad','Gadag','Hassan','Haveri','Kalaburagi','Kodagu','Kolar',
               'Koppal','Mandya','Mysuru','Raichur','Ramanagara','Shivamogga','Tumakuru',
               'Udupi','Uttara Kannada','Vijayapura','Vijayanagara','Yadgir']) AS t(d)
WHERE name = 'Karnataka';

-- ─────────────────────────────────────────────────────────────────────────────
-- 12. KERALA (KL)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Kerala', 'KL');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Alappuzha','Ernakulam','Idukki','Kannur','Kasaragod','Kollam','Kottayam',
               'Kozhikode','Malappuram','Palakkad','Pathanamthitta','Thiruvananthapuram',
               'Thrissur','Wayanad']) AS t(d)
WHERE name = 'Kerala';

-- ─────────────────────────────────────────────────────────────────────────────
-- 13. MADHYA PRADESH (MP)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Madhya Pradesh', 'MP');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Agar Malwa','Alirajpur','Anuppur','Ashoknagar','Balaghat','Barwani','Betul',
               'Bhind','Bhopal','Burhanpur','Chhatarpur','Chhindwara','Damoh','Datia',
               'Dewas','Dhar','Dindori','Guna','Gwalior','Harda','Hoshangabad','Indore',
               'Jabalpur','Jhabua','Katni','Khandwa','Khargone','Mandla','Mandsaur',
               'Morena','Mungeli','Narsinghpur','Neemuch','Niwari','Panna','Raisen',
               'Rajgarh','Ratlam','Rewa','Sagar','Satna','Sehore','Seoni','Shahdol',
               'Shajapur','Sheopur','Shivpuri','Sidhi','Singrauli','Tikamgarh','Ujjain',
               'Umaria','Vidisha','Maihar','Chachaura','Nagda']) AS t(d)
WHERE name = 'Madhya Pradesh';

-- ─────────────────────────────────────────────────────────────────────────────
-- 14. MAHARASHTRA (MH)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Maharashtra', 'MH');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Ahmednagar','Akola','Amravati','Aurangabad','Beed','Bhandara','Buldhana',
               'Chandrapur','Dhule','Gadchiroli','Gondia','Hingoli','Jalgaon','Jalna',
               'Kolhapur','Latur','Mumbai City','Mumbai Suburban','Nagpur','Nanded',
               'Nandurbar','Nashik','Osmanabad','Palghar','Parbhani','Pune','Raigad',
               'Ratnagiri','Sangli','Satara','Sindhudurg','Solapur','Thane','Wardha',
               'Washim','Yavatmal']) AS t(d)
WHERE name = 'Maharashtra';

-- ─────────────────────────────────────────────────────────────────────────────
-- 15. MANIPUR (MN)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Manipur', 'MN');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Bishnupur','Chandel','Churachandpur','Imphal East','Imphal West','Jiribam',
               'Kakching','Kamjong','Kangpokpi','Noney','Pherzawl','Senapati','Tamenglong',
               'Tengnoupal','Thoubal','Ukhrul']) AS t(d)
WHERE name = 'Manipur';

-- ─────────────────────────────────────────────────────────────────────────────
-- 16. MEGHALAYA (ML)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Meghalaya', 'ML');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Eastern West Khasi Hills','East Garo Hills','East Jaintia Hills','East Khasi Hills',
               'North Garo Hills','Ri Bhoi','South Garo Hills','South West Garo Hills',
               'South West Khasi Hills','West Garo Hills','West Jaintia Hills','West Khasi Hills']) AS t(d)
WHERE name = 'Meghalaya';

-- ─────────────────────────────────────────────────────────────────────────────
-- 17. MIZORAM (MZ)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Mizoram', 'MZ');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Aizawl','Champhai','Hnahthial','Khawzawl','Kolasib','Lawngtlai','Lunglei',
               'Mamit','Saiha','Saitual','Serchhip']) AS t(d)
WHERE name = 'Mizoram';

-- ─────────────────────────────────────────────────────────────────────────────
-- 18. NAGALAND (NL)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Nagaland', 'NL');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Chumoukedima','Dimapur','Kiphire','Kohima','Longleng','Mokokchung','Mon',
               'Niuland','Noklak','Peren','Phek','Shamator','Tseminyü','Tuensang','Wokha','Zunheboto']) AS t(d)
WHERE name = 'Nagaland';

-- ─────────────────────────────────────────────────────────────────────────────
-- 19. ODISHA (OD)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Odisha', 'OD');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Angul','Balangir','Balasore','Bargarh','Bhadrak','Boudh','Cuttack','Deogarh',
               'Dhenkanal','Gajapati','Ganjam','Jagatsinghpur','Jajpur','Jharsuguda',
               'Kalahandi','Kandhamal','Kendrapara','Kendujhar','Khordha','Koraput',
               'Malkangiri','Mayurbhanj','Nabarangpur','Nayagarh','Nuapada','Puri',
               'Rayagada','Sambalpur','Subarnapur','Sundargarh']) AS t(d)
WHERE name = 'Odisha';

-- ─────────────────────────────────────────────────────────────────────────────
-- 20. PUNJAB (PB)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Punjab', 'PB');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Amritsar','Barnala','Bathinda','Faridkot','Fatehgarh Sahib','Fazilka',
               'Ferozepur','Gurdaspur','Hoshiarpur','Jalandhar','Kapurthala','Ludhiana',
               'Malerkotla','Mansa','Moga','Mohali','Muktsar','Nawanshahr','Pathankot',
               'Patiala','Rupnagar','Sangrur','Tarn Taran']) AS t(d)
WHERE name = 'Punjab';

-- ─────────────────────────────────────────────────────────────────────────────
-- 21. RAJASTHAN (RJ)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Rajasthan', 'RJ');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Ajmer','Alwar','Anupgarh','Balotra','Banswara','Baran','Barmer',
               'Beawar','Bharatpur','Bhilwara','Bikaner','Bundi','Chittorgarh','Churu',
               'Dausa','Deeg','Dholpur','Didwana-Kuchaman','Dudu','Dungarpur',
               'Gangapur City','Hanumangarh','Jaipur','Jaipur Rural','Jaisalmer',
               'Jalore','Jhalawar','Jhunjhunu','Jodhpur','Jodhpur Rural','Karauli',
               'Kekri','Khairthal-Tijara','Kotputli-Behror','Kota','Nagaur','Neem ka Thana',
               'Pali','Phalodi','Pratapgarh','Rajsamand','Salumbar','Sanchore',
               'Sawai Madhopur','Shahpura','Sikar','Sirohi','Sri Ganganagar','Tonk','Udaipur']) AS t(d)
WHERE name = 'Rajasthan';

-- ─────────────────────────────────────────────────────────────────────────────
-- 22. SIKKIM (SK)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Sikkim', 'SK');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['East Sikkim','Gyalshing','Namchi','Pakyong','Soreng','North Sikkim']) AS t(d)
WHERE name = 'Sikkim';

-- ─────────────────────────────────────────────────────────────────────────────
-- 23. TAMIL NADU (TN)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Tamil Nadu', 'TN');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Ariyalur','Chengalpattu','Chennai','Coimbatore','Cuddalore','Dharmapuri',
               'Dindigul','Erode','Kallakurichi','Kancheepuram','Kanyakumari','Karur',
               'Krishnagiri','Madurai','Mayiladuthurai','Nagapattinam','Namakkal',
               'Nilgiris','Perambalur','Pudukkottai','Ramanathapuram','Ranipet','Salem',
               'Sivaganga','Tenkasi','Thanjavur','Theni','Thoothukudi','Tiruchirappalli',
               'Tirunelveli','Tirupathur','Tiruppur','Tiruvallur','Tiruvannamalai',
               'Tiruvarur','Vellore','Viluppuram','Virudhunagar']) AS t(d)
WHERE name = 'Tamil Nadu';

-- ─────────────────────────────────────────────────────────────────────────────
-- 24. TELANGANA (TS)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Telangana', 'TS');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Adilabad','Bhadradri Kothagudem','Hanumakonda','Hyderabad','Jagitial',
               'Jangaon','Jayashankar Bhupalpally','Jogulamba Gadwal','Kamareddy','Karimnagar',
               'Khammam','Kumuram Bheem Asifabad','Mahabubabad','Mahabubnagar','Mancherial',
               'Medak','Medchal-Malkajgiri','Mulugu','Nagarkurnool','Nalgonda','Narayanpet',
               'Nirmal','Nizamabad','Peddapalli','Rajanna Sircilla','Ranga Reddy','Sangareddy',
               'Siddipet','Suryapet','Vikarabad','Wanaparthy','Warangal','Yadadri Bhuvanagiri']) AS t(d)
WHERE name = 'Telangana';

-- ─────────────────────────────────────────────────────────────────────────────
-- 25. TRIPURA (TR)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Tripura', 'TR');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Dhalai','Gomati','Khowai','North Tripura','Sepahijala','South Tripura',
               'Unakoti','West Tripura']) AS t(d)
WHERE name = 'Tripura';

-- ─────────────────────────────────────────────────────────────────────────────
-- 26. UTTAR PRADESH (UP)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Uttar Pradesh', 'UP');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Agra','Aligarh','Ambedkar Nagar','Amethi','Amroha','Auraiya','Ayodhya',
               'Azamgarh','Baghpat','Bahraich','Ballia','Balrampur','Banda','Barabanki',
               'Bareilly','Basti','Bhadohi','Bijnor','Budaun','Bulandshahr','Chandauli',
               'Chitrakoot','Deoria','Etah','Etawah','Farrukhabad','Fatehpur','Firozabad',
               'Gautam Buddha Nagar','Ghaziabad','Ghazipur','Gonda','Gorakhpur','Hamirpur',
               'Hapur','Hardoi','Hathras','Jalaun','Jaunpur','Jhansi','Kannauj',
               'Kanpur Dehat','Kanpur Nagar','Kasganj','Kaushambi','Kushinagar',
               'Lakhimpur Kheri','Lalitpur','Lucknow','Maharajganj','Mahoba','Mainpuri',
               'Mathura','Mau','Meerut','Mirzapur','Moradabad','Muzaffarnagar','Pilibhit',
               'Pratapgarh','Prayagraj','Rae Bareli','Rampur','Saharanpur','Sambhal',
               'Sant Kabir Nagar','Shahjahanpur','Shamli','Shravasti','Siddharthnagar',
               'Sitapur','Sonbhadra','Sultanpur','Unnao','Varanasi']) AS t(d)
WHERE name = 'Uttar Pradesh';

-- ─────────────────────────────────────────────────────────────────────────────
-- 27. UTTARAKHAND (UK)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('Uttarakhand', 'UK');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Almora','Bageshwar','Chamoli','Champawat','Dehradun','Haridwar','Nainital',
               'Pauri Garhwal','Pithoragarh','Rudraprayag','Tehri Garhwal','Udham Singh Nagar',
               'Uttarkashi']) AS t(d)
WHERE name = 'Uttarakhand';

-- ─────────────────────────────────────────────────────────────────────────────
-- 28. WEST BENGAL (WB)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO india_states (name, code) VALUES ('West Bengal', 'WB');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Alipurduar','Bankura','Birbhum','Cooch Behar','Dakshin Dinajpur','Darjeeling',
               'Hooghly','Howrah','Jalpaiguri','Jhargram','Kalimpong','Kolkata','Malda',
               'Murshidabad','Nadia','North 24 Parganas','Paschim Bardhaman','Paschim Medinipur',
               'Purba Bardhaman','Purba Medinipur','Purulia','South 24 Parganas',
               'Uttar Dinajpur']) AS t(d)
WHERE name = 'West Bengal';

-- ─────────────────────────────────────────────────────────────────────────────
-- UNION TERRITORIES
-- ─────────────────────────────────────────────────────────────────────────────

-- 29. ANDAMAN AND NICOBAR ISLANDS (AN)
INSERT INTO india_states (name, code) VALUES ('Andaman and Nicobar Islands', 'AN');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Nicobars','North and Middle Andaman','South Andaman']) AS t(d)
WHERE name = 'Andaman and Nicobar Islands';

-- 30. CHANDIGARH (CH)
INSERT INTO india_states (name, code) VALUES ('Chandigarh', 'CH');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Chandigarh']) AS t(d)
WHERE name = 'Chandigarh';

-- 31. DADRA AND NAGAR HAVELI AND DAMAN AND DIU (DH)
INSERT INTO india_states (name, code) VALUES ('Dadra and Nagar Haveli and Daman and Diu', 'DH');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Dadra and Nagar Haveli','Daman','Diu']) AS t(d)
WHERE name = 'Dadra and Nagar Haveli and Daman and Diu';

-- 32. DELHI (DL)
INSERT INTO india_states (name, code) VALUES ('Delhi', 'DL');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Central Delhi','East Delhi','New Delhi','North Delhi','North East Delhi',
               'North West Delhi','Shahdara','South Delhi','South East Delhi','South West Delhi',
               'West Delhi']) AS t(d)
WHERE name = 'Delhi';

-- 33. JAMMU AND KASHMIR (JK)
INSERT INTO india_states (name, code) VALUES ('Jammu and Kashmir', 'JK');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Anantnag','Bandipora','Baramulla','Budgam','Doda','Ganderbal','Jammu',
               'Kathua','Kishtwar','Kulgam','Kupwara','Poonch','Pulwama','Rajouri',
               'Ramban','Reasi','Samba','Shopian','Srinagar','Udhampur']) AS t(d)
WHERE name = 'Jammu and Kashmir';

-- 34. LADAKH (LA)
INSERT INTO india_states (name, code) VALUES ('Ladakh', 'LA');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Kargil','Leh']) AS t(d)
WHERE name = 'Ladakh';

-- 35. LAKSHADWEEP (LD)
INSERT INTO india_states (name, code) VALUES ('Lakshadweep', 'LD');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Lakshadweep']) AS t(d)
WHERE name = 'Lakshadweep';

-- 36. PUDUCHERRY (PY)
INSERT INTO india_states (name, code) VALUES ('Puducherry', 'PY');
INSERT INTO india_districts (state_id, name) SELECT id, d FROM india_states,
  UNNEST(ARRAY['Karaikal','Mahe','Puducherry','Yanam']) AS t(d)
WHERE name = 'Puducherry';

