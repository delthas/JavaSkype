// Wolfssl should be compiled with ./configure --enable-md5 --enable-rsa --enable-sha --enable-aes --enable-opensslextra --enable-fortress --disable-examples
#include "wolfssl.h"
#include <wolfssl/wolfcrypt/random.h>
#include <wolfssl/wolfcrypt/rsa.h>

int RSA_private_encrypt(word32 inLen, const byte *in,
    byte *out, RSA *key, int padding)
{
	return wc_RsaSSL_Sign(in, 0, out, inLen, (RsaKey*)key->internal, NULL);
}

int AES_set_encrypt_key (const unsigned char *userKey, const int bits, AES_KEY *key)
{
	memcpy(key->key,userKey,(key->bytes=bits/8));
}

void AES_ctr128_encrypt(const byte *in, byte *out, word32 len, AES_KEY *key,
    byte *ivec, byte *ecount_buf, unsigned int *num)
{
	Aes aes;
	byte RecvBuf[0x1000];

	wc_AesSetKey(&aes, key->key, key->bytes, ivec, AES_ENCRYPTION);
	wc_AesCtrEncrypt(&aes, RecvBuf, in, len);
	memcpy(out, RecvBuf, len);
}

int RSA_public_encrypt(word32 len, byte* fr, byte* to, RSA* rsa, int padding)
{
	RsaKey        *rsaKey = (RsaKey*)rsa->internal, rsaKeyBak;
	int           ret;

	memcpy(&rsaKeyBak, rsaKey, sizeof(rsaKeyBak));
	rsaKey->n = *(mp_int*)rsa->n->internal;
	rsaKey->e = *(mp_int*)rsa->e->internal;
	ret = wc_RsaPublicEncrypt(fr, 0, to, len, rsaKey, NULL);
	memcpy(rsaKey, &rsaKeyBak, sizeof(rsaKeyBak));
	return ret;
}
