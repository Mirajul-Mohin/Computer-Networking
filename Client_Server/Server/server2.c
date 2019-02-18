#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#define PORT 8080

int main(){
    int port=8080;
    //creating socket
    int server_socket= socket(AF_INET,SOCK_STREAM,0);

    if(server_socket<0){
        printf("Socket creation failed\n");
        return 0;
    }

    //creating an address
    struct sockaddr_in address;

    address.sin_family= AF_INET;
    address.sin_port= htons(8080);
    address.sin_addr.s_addr= INADDR_ANY;

    //bind
    bind(server_socket,(struct sockaddr*) &address, sizeof(address));

    //listen and accept
    listen(server_socket,3);

    //accept and return new FD referring to the connected socket
    int client_socket= accept(server_socket, NULL,NULL);

    char welcome_msg[100]="Welcome! You are now ready to send email :)";
    char status_code[100]= "2020";

    if(client_socket>=0){
        send(client_socket,&welcome_msg,sizeof(welcome_msg),0);
        send(client_socket,&status_code,sizeof(status_code),0);
    }

    //send
    char data[100]= "Hello Client!";
    send(client_socket,&data,sizeof(data),0);

    FILE *fp;
    fp= fopen("bob2.txt","a");

    //recieve
    char subject[100];
    recv(client_socket,&subject,sizeof(subject),0);
    printf("Subject: %s\n",subject);

    fprintf(fp,"Subject: %s\n",subject);



    char body[100];

    recv(client_socket,&body,sizeof(body),0);
    printf("email body: %s",body);
    fprintf(fp,"Body: %s",body);


}
