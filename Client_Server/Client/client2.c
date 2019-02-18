#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#include<netdb.h>
#include <arpa/inet.h>



int main(int argc, char* argv[]){


    if(argc!=4){
        printf("Incorrect number of arguments: Operation Aborted.\n");
        //return;
    }

    char hostname[100], port[100];
    int k=0, l=0;
    int flag=0;
    for(int i=0;i<strlen(argv[1]);i++){
        if(flag==2){
            port[l++]=argv[1][i];
        }
        if(argv[1][i]==':'){
            hostname[k++]='\0';
            flag=2;
        }
        if(flag==1){
            hostname[k++]=argv[1][i];
        }
        if(argv[1][i]=='@')
            flag=1;
    }
    port[l++]='\0';
    printf("Host: %s\nPort: %s\n",hostname, port);

    struct hostent *ghbn= gethostbyname(hostname);
    //char* IP= inet_ntoa(*(long*)ghbn->h_addr_list[0]);

    //printf("IP Address: %s\n",IP);


    int network_socket= socket(AF_INET,SOCK_STREAM,0);

    if(network_socket<0){
        puts("There was an error creating the socket");
    }

    //creating the server address
    struct sockaddr_in server_address;

    server_address.sin_family= AF_INET;
    server_address.sin_port= htons(8080);
    server_address.sin_addr.s_addr= *(long*)ghbn->h_addr_list[0];


    //connect
    int connection= connect(network_socket, (struct sockaddr*) &server_address, sizeof(server_address));

    if(connection==-1){
        puts("There's a problem trying to connect");
        return 0;
    }

    //recive message
    char welcome_msg[100];
    recv(network_socket,&welcome_msg,sizeof(welcome_msg),0);
    printf("Welcome Message recieved:\n%s\n",welcome_msg);

    char status_code[100];
    recv(network_socket,&status_code,sizeof(status_code),0);
    printf("Status Code Message recieved:\n%s\n",status_code);

    if(status_code[0]!='2'){
        printf("Wrong status code starting with:%c\n",status_code[0]);
        return 0;
    }

    //send email
    char subject[100]="Bob's howabouts";
    send(network_socket,&subject,sizeof(subject),0);



    char message[100][150],buffer[150];
    int length=0;
    FILE *fp;

    fp=fopen("bob.txt", "r");

    /*stores the data from the string*/
    while(fgets(buffer,150,fp)){
        strcpy(message[length],buffer);
        length++;
    }

    fp=fopen("bob3.txt", "a");
    for(int i=0; i<length;i++){
        printf("%s",message[i]);
        fprintf(fp,"%s",message[i]);
        send(network_socket,&message[i],sizeof(message[i]),0);
    }






}
