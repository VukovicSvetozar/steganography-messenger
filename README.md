<br>
<br>
<div align="center">
  <h1>Steganography Messenger</h1> 
</div>
<br>
<br>
<br>

<div style="page-break-before: always;"></div>

**Steganography Messenger** je desktop aplikacija koja korisnicima omogućava razmjenu poruka na način da se svaka poruka prije slanja šifruje i sakrije unutar slike upotrebom steganografije – tako da niko osim primaoca ne može saznati ni da poruka postoji, a kamoli pročitati njen sadržaj.

Aplikacija demonstrira praktičnu primjenu infrastrukture javnog ključa (PKI), hibridne enkripcije, digitalnog potpisivanja i LSB steganografije implementirane od nule, bez oslanjanja na gotove steganografske biblioteke.

---

## 🛠️ Ključne Funkcionalnosti

### 1. Autentifikacija i infrastruktura javnog ključa (PKI)
* **Prijava korisnika:** Lozinke se ne čuvaju u čitljivom obliku – heširaju se algoritmom `PBKDF2WithHmacSHA512` (10.000 iteracija) uz jedinstven `salt` po korisniku prije upisa u evidenciju naloga.
* **Zajedničko CA tijelo:** Svaki korisnik ima X.509 sertifikat izdat od strane zajedničkog Root CA. Prilikom prijave, kao i prilikom slanja poruke, aplikacija provjerava da li je sertifikat (sopstveni, odnosno sertifikat primaoca) potpisan od tog CA tijela i da nije istekao.
* **Provjera namjene ključa:** Prije potpisivanja i enkripcije provjerava se `KeyUsage` ekstenzija sertifikata (`digitalSignature`, `keyEncipherment`), čime se sprečava upotreba ključa izvan njegove predviđene namjene.
* **Nalozi se kreiraju van aplikacije:** Novi korisnički nalog se ne otvara kroz aplikaciju, već se ručno dodaje u evidenciju; pomoćna klasa `ProtectUserPassword` generiše bezbjedan, soljeni heš lozinke koji se unosi u zapis.

### 2. Hibridna enkripcija i digitalni potpis
* **AES + RSA:** Poruka se šifruje slučajno generisanim AES-128 ključem, koji se zatim šifruje RSA (2048-bit) javnim ključem primaoca izvučenim iz njegovog sertifikata – samo primalac svojim privatnim ključem može doći do AES ključa, a time i do sadržaja poruke.
* **Digitalni potpis:** Prije enkripcije pošiljalac potpisuje poruku svojim privatnim ključem (`SHA256withRSA`). Nakon dekripcije, primalac verifikuje potpis javnim ključem pošiljaoca i tako potvrđuje njegov identitet i integritet poruke.
* **Priprema za skrivanje:** Šifrovan AES ključ i šifrovan (potpisan) sadržaj poruke se Base64 kodiraju i dodatno GZIP kompresuju, čime se smanjuje broj piksela potreban za skrivanje u slici.

### 3. LSB Steganografija (sopstvena implementacija)
* **Skrivanje u najmanje značajnim bitovima:** Pripremljeni šifrat se bit po bit upisuje u najmanje značajne bitove (LSB) odabranih kanala boje slike nosioca, tako da se kvalitet slike vizuelno ne mijenja.
* **Podesivi parametri:** Korisnik bira koje kanale boje (R, G, B – pojedinačno ili kombinovano) i koliko bitova po kanalu (1–8) koristi za skrivanje poruke, uz prikaz potrebnog i raspoloživog broja piksela u realnom vremenu.
* **Zaglavlje poruke:** Nezavisno od korisnikovog izbora, na početak slike se uvijek upisuje kratko zaglavlje (RGB kanali, 2 LSB-a) koje čuva odabrane parametre kodiranja, kako bi primalac mogao ispravno dekodirati poruku.
* **Provjera veličine nosioca:** Ako odabrana slika nema dovoljno piksela za odabranu poruku i parametre, aplikacija to prijavljuje i izračunava minimalnu potrebnu veličinu slike.

### 4. Privatnost, anonimnost i integritet poruka
* **Anonimnost do dekripcije:** Do trenutka dekripcije aplikacija ne otkriva ko je pošiljalac – ti podaci postoje isključivo unutar šifrovanog sadržaja slike. Čak je i lokacija korisničkog foldera na disku imenovana SHA-224 heš vrijednošću imena i prezimena korisnika, a ne njegovim stvarnim imenom.
* **Čitanje samo jednom:** Nakon uspješne dekripcije, slika koja je sadržavala poruku trajno se briše sa sistema.
* **Detekcija spoljašnjeg brisanja:** Ako neko sa direktnim pristupom fajl sistemu obriše sliku mimo aplikacije, to se registruje pri sljedećem pokušaju čitanja i korisnik biva o tome obaviješten, umjesto da aplikacija samo prijavi grešku.
* **Evidencija bez sadržaja:** Aplikacija vodi evidenciju isključivo o broju pristiglih poruka (tj. o putanjama do slika); vrijeme slanja, pošiljalac i sadržaj poruke postaju vidljivi tek nakon dekripcije kod primaoca.

---

## 💻 Tehnologije i Alati

* **Jezik:** Java – JavaFX GUI bez modularnog sistema (nema `module-info.java`)
* **Kriptografija:** Java Cryptography Architecture (JCA/JCE) uz **Bouncy Castle** (`bcprov-jdk15on`) kao provajder – AES-128, RSA-2048, `SHA256withRSA` digitalni potpis, `PBKDF2WithHmacSHA512` heširanje lozinki, SHA-224 heširanje identiteta
* **Sertifikati:** X.509 sertifikati, PKCS8 privatni ključevi; PKI infrastruktura pripremljena eksternim alatom (**XCA**)
* **Slike:** `javax.imageio` za učitavanje i upis slika; sopstvena LSB implementacija umjesto gotove steganografske biblioteke
* **Ostalo:** GZIP kompresija poruka, `java.util.logging` za bilježenje grešaka u `log.xml`

---

## 🚀 Kako Pokrenuti Projekat Lokalno

### Preduslovi
* **JDK 8** – projekat ne koristi Java modularni sistem, pa je najjednostavnije pokrenuti ga upravo sa JDK 8, gdje je JavaFX još uvijek bio ugrađen u sam JDK. Uz noviji JDK potrebno je posebno preuzeti JavaFX SDK i dodati VM argumente (`--module-path <putanja-do-javafx-sdk>/lib --add-modules javafx.controls,javafx.fxml`).
* IDE sa podrškom za JavaFX/FXML (npr. **Eclipse**).
* Biblioteke iz `app/lib` dodate na build path IDE-a – u kodu se aktivno koristi samo `bcprov-jdk15on-159.jar` (Bouncy Castle).

### Priprema PKI infrastrukture i naloga
Sertifikati priloženi u repozitorijumu su demonstracioni i istekli, pa je za stvarno korišćenje potrebno pripremiti novu infrastrukturu:
1. Kreirati Root CA i izdati X.509 sertifikat za svakog korisnika (npr. pomoću **XCA** alata ili `keytool`/`openssl`), sa `KeyUsage` ekstenzijom koja uključuje `digitalSignature` i `keyEncipherment`, i sačuvati ga kao `Sertifikati/Korisnicki sertifikati/ImePrezime.cer`.
2. Dodati novi red u `Lista korisnika/Lista korisnika.txt`, prema formatu opisanom u zaglavlju te datoteke (korisničko ime, ime, prezime, salt i heš lozinke) – salt i heš se mogu generisati pomoćnom klasom `ProtectUserPassword`.
3. Sačuvati privatni ključ korisnika u PKCS8 (`.der`) formatu na putanju `Korisnici/{SHA-224(ime+prezime)}/Privatni kljuc/PrivatniKljuc.der`.

### Pokretanje
Pokrenuti klasu `pokretanje.PokretanjeAplikacije` (sadrži `main` metodu) kao Java aplikaciju iz IDE-a.
