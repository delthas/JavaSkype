/*  
 * Skype Login
 * 
 * Based on:
 *   FakeSkype : Skype reverse engineering proof-of-concept client
 *               Ouanilo MEDEGAN (c) 2006   http://www.oklabs.net
 *   pyskype   : Skype login Python script by uunicorn
 *
 * Written by:   leecher@dose.0wnz.at (c) 2015 
 *
 * Module:       UNIX platform specific functions
 *
 */
#include <sys/ioctl.h>
#include <sys/utsname.h>
#include <sys/types.h>
#include <net/if.h>
#include <stropts.h>
#include <stdio.h>
#include <unistd.h>
#include <pwd.h>
#include <limits.h>
#include "common.h"
#include "random.h"

static int getMACAddr(unsigned char *pResult)
{
	int res = 0;
	struct ifreq ifr;
	struct ifconf ifc;
	char *pszIfs[] = {"eth0", "eth1", "eth2", "eth3", "wlan0", "wlan1", "usb0", "usb1", "wmaster0"};
	char buf[1024];
	int i, fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);

	if (fd != -1)
	{
		for (i=0; i<sizeof(pszIfs)/sizeof(pszIfs[0]); i++)
		{
			memset(&ifr, 0, sizeof(ifr));
			strcpy(ifr.ifr_name, pszIfs[i]);
			if (ioctl(fd, SIOCGIFHWADDR, &ifr) == 0)
			{
				memcpy(pResult, ifr.ifr_hwaddr.sa_data, 6);
				close(fd);
				return 1;
			}
		}
		ifc.ifc_len = sizeof(buf);
		ifc.ifc_buf = buf;
		if (ioctl(fd, SIOCGIFCONF, &ifc) != -1)
		{
			for (i=0; i<(ifc.ifc_len / sizeof(struct ifreq)); i++)
			{
				memset(&ifr, 0, sizeof(ifr));
				strcpy(ifr.ifr_name, ifc.ifc_req[i].ifr_name);
				if (ioctl(fd, SIOCGIFFLAGS, &ifr) == 0)
				{
					if (! (ifr.ifr_flags & IFF_LOOPBACK))
					{
						if (ioctl(fd, SIOCGIFHWADDR, &ifr) == 0)
						{
							memcpy(pResult, ifr.ifr_hwaddr.sa_data, 6);
							close(fd);
							return 1;
						}
					}
				}
			}
		}
		close(fd);
	}
	return 0;
}

static const char *getConfigDir()
{
	const char *homedir;

	if (!(homedir = getenv("HOME")) && !(homedir = getenv("HOME")) &&
		!(homedir = getpwuid(getuid())->pw_dir)) homedir=".";
	return homedir;
}

int64_t PlatFormSpecific()
{
	unsigned char	mac_addr[6];

	if (getMACAddr(mac_addr))
		return BytesSHA1I64(mac_addr, sizeof(mac_addr));
	else
	{
		FILE *fp;
		size_t rd;
		SHA_CTX	Context;
		unsigned char buf[255];
		uchar Buffer[SHA_DIGEST_LENGTH];
		int i;
		char *pszFiles[] = {"/etc/issue", "/etc/hostname", "/etc/HOSTNAME", "/etc/conf.d/hostname", "/etc/resolv.conf"};

		SHA1_Init(&Context);
		for (i=0; i<sizeof(pszFiles)/sizeof(pszFiles[0]); i++)
		{
			if (fp=fopen(pszFiles[i], "r"))
			{
				if ((rd = fread(buf, 1, sizeof(buf)-1, fp)))
					SHA1_Update(&Context, buf, rd);
				fclose(fp);
			}
		}
		SHA1_Final(Buffer, &Context);
		return *(int64_t *)Buffer;
	}
}

void	 InitNodeId(Skype_Inst *pInst)
{
	FILE *fp;
	int64_t NodeID;
	int ok=0;
	char  szDir[PATH_MAX], *p;

	*(int64_t *)&pInst->NodeID = BytesRandomI64();
	p = szDir + sprintf (szDir, "%s/.SkyLogin", getConfigDir());
	strcpy (p, "/NodeID");
	if (fp=fopen(szDir, "rb"))
	{
		if ((ok = fread(&NodeID, sizeof(NodeID), 1, fp)) == 1)
			*(int64_t*)pInst->NodeID = NodeID;
		fclose(fp);
	}
	if (!ok)
	{
		*p=0;
		mkdir(szDir, 0755);
		*p='/';
		if (fp=fopen(szDir, "wb"))
		{
			fwrite(&pInst->NodeID, sizeof(NodeID), 1, fp);
			fclose(fp);
		}
	}
}

Memory_U Credentials_Load(char *pszUser)
{
	Memory_U creds={0};
	FILE *fp;
	char  szKey[PATH_MAX];

	sprintf (szKey, "%s/.SkyLogin/%s/Credentials", getConfigDir(), pszUser);
	if (fp = fopen (szKey, "r"))
	{
		fseek(fp, 0, SEEK_END);
		creds.MsZ = ftell(fp);
		fseek(fp, 0, SEEK_SET);
		if (!(creds.Memory = malloc(creds.MsZ)))
			creds.MsZ = 0;
		if (creds.Memory && fread(creds.Memory, creds.MsZ, 1, fp)!=1)
		{
			free(creds.Memory);
			memset(&creds, 0, sizeof(creds));
		}
		fclose(fp);
	}
	return creds;
}

int Credentials_Save(Memory_U creds, char *pszUser)
{
	FILE *fp;
	int iRet = 0;
	char  szKey[PATH_MAX];

	sprintf (szKey, "%s/.SkyLogin/", getConfigDir());
	mkdir(szKey, 0755);
	strcat (szKey, pszUser);
	mkdir(szKey, 0755);
	strcat (szKey, "/Credentials");
	if (fp = fopen (szKey, "w"))
	{
		iRet = fwrite(creds.Memory, creds.MsZ, 1, fp);
		fclose(fp);
	}
	return iRet;
}

void FillMiscDatas(Skype_Inst *pInst, unsigned int *Datas)
{
	FILE *fp;
	SHA_CTX	Context;
	uchar	Buffer[SHA_DIGEST_LENGTH];
	char buffer[512], *p, d=0;
	unsigned char	mac_addr[6];
	struct utsname name;

	SHA1_Init(&Context);
	SHA1_Update(&Context, "cpuinfo", 7);
	if ((fp=fopen("/proc/cpuinfo","rt")))
	{
		while (!feof(fp) && !ferror(fp))
		{
			if (fgets(buffer, sizeof(buffer), fp))
			{
				for (p=buffer; *p; p++) if (isupper(*p)) *p=tolower(*p);
				if (strstr(buffer, "model") ||
					strstr(buffer, "flags") ||
					strstr(buffer, "processor") ||
					strstr(buffer, "features") ||
					strstr(buffer, "architecture"))
				{
					SHA1_Update(&Context, buffer, p-buffer);
				}
			}
		}
		fclose(fp);
	}
	SHA1_Final(Buffer, &Context);
	Datas[d++]=*(unsigned int *)Buffer;

	SHA1_Init(&Context);
	SHA1_Update(&Context, "meminfo", 7);
	if ((fp=fopen("/proc/meminfo","rt")))
	{
		while (!feof(fp) && !ferror(fp))
		{
			if (fgets(buffer, sizeof(buffer), fp))
			{
				for (p=buffer; *p; p++) if (isupper(*p)) *p=tolower(*p);
				if (strstr(buffer, "memtotal") ||
					strstr(buffer, "swaptotal"))
				{
					SHA1_Update(&Context, buffer, p-buffer);
				}
			}
		}
		fclose(fp);
	}
	SHA1_Final(Buffer, &Context);
	Datas[d++]=*(unsigned int *)Buffer;

	if (getMACAddr(mac_addr))
	{
		SHA1_Init(&Context);
		SHA1_Update(&Context, mac_addr, sizeof(mac_addr));
		SHA1_Final(Buffer, &Context);
		Datas[d++]=*(unsigned int *)Buffer;
	}

	SHA1_Init(&Context);
	SHA1_Update(&Context, "uname", 5);
	if (uname(&name) == 0)
	{
		SHA1_Update(&Context, name.sysname, strlen(name.sysname));
		SHA1_Update(&Context, name.nodename, strlen(name.nodename));
		SHA1_Update(&Context, name.machine, strlen(name.machine));
	}
	SHA1_Final(Buffer, &Context);
	Datas[d++]=*(unsigned int *)Buffer;
}

void FillRndBuffer(unsigned char *Buffer)
{
	int fd = open("/dev/urandom", 0);

	if (fd!=-1)
	{
		read(fd, Buffer, 0x464);
		close(fd);
	}
}

