#ifndef __WOLFSSL_H__
#define __WOLFSSL_H__
#include <wolfssl/options.h>
#include <wolfssl/ssl.h>
#include <wolfssl/openssl/md5.h>
#include <wolfssl/openssl/rsa.h>
#include <wolfssl/openssl/sha.h>
#include <wolfssl/openssl/evp.h>

/* WOLFSSL has no RC4 implementation? */
#undef USE_RC4

/* Fix some missing defs from wolfssl OpenSSL Compat layer */
#ifndef MD5_DIGEST_LENGTH
#define MD5_DIGEST_LENGTH 16
#endif
#ifndef RSA_NO_PADDING
#define RSA_NO_PADDING          3
#endif
#ifndef AES_BLOCK_SIZE
#define AES_BLOCK_SIZE 16
#endif

/* Missing RSA function support */
int RSA_private_encrypt(word32 inLen, const byte *in,
    byte *out, RSA *key, int padding);
#undef RSA_public_encrypt
int RSA_public_encrypt(word32 len, byte* fr, byte* to, RSA* rsa, int padding);


/* AES support specifically for AES-128 CTR */
typedef struct {
  word32 bytes;
  byte key[32];
} AES_KEY;

int AES_set_encrypt_key (const byte *userKey, const int bits, AES_KEY *key);
void AES_ctr128_encrypt(const byte *in, byte *out, word32 len, AES_KEY *key,
    byte ivec[AES_BLOCK_SIZE], byte ecount_buf[AES_BLOCK_SIZE], unsigned int *num);
#endif
