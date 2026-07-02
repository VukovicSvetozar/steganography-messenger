package model;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class Korisnik {

	private String ime;
	private String prezime;
	private int brojNeprocitanihPoruka;

	private String putanjaSertifikata;
	private String putanjaKorisnickogFoldera;

	private X509Certificate sertifikat;
	private PrivateKey privatniKljuc;

	public Korisnik() {
		super();
	}

	public Korisnik(String ime, String prezime) {
		super();
		this.ime = ime;
		this.prezime = prezime;
	}

	@Override
	public String toString() {
		return ime + " " + prezime;
	}

	public String getIme() {
		return ime;
	}

	public void setIme(String ime) {
		this.ime = ime;
	}

	public String getPrezime() {
		return prezime;
	}

	public void setPrezime(String prezime) {
		this.prezime = prezime;
	}

	public String getPutanjaKorisnickogFoldera() {
		return putanjaKorisnickogFoldera;
	}

	public void setPutanjaKorisnickogFoldera(String putanjaKorisnickogFoldera) {
		this.putanjaKorisnickogFoldera = putanjaKorisnickogFoldera;
	}

	public X509Certificate getSertifikat() {
		return sertifikat;
	}

	public void setSertifikat(X509Certificate sertifikat) {
		this.sertifikat = sertifikat;
	}

	public PrivateKey getPrivatniKljuc() {
		return privatniKljuc;
	}

	public void setPrivatniKljuc(PrivateKey privatniKljuc) {
		this.privatniKljuc = privatniKljuc;
	}

	public int getBrojNeprocitanihPoruka() {
		return brojNeprocitanihPoruka;
	}

	public void setBrojNeprocitanihPoruka(int brojNeprocitanihPoruka) {
		this.brojNeprocitanihPoruka = brojNeprocitanihPoruka;
	}

	public String getPutanjaSertifikata() {
		return putanjaSertifikata;
	}

	public void setPutanjaSertifikata(String putanjaSertifikata) {
		this.putanjaSertifikata = putanjaSertifikata;
	}

}
