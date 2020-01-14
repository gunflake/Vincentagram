package com.example.vincentagram.navigation.model

data class AlarmDTO(var destinationUid : String? = null,
                    var uid : String? = null,
                    var email : String? = null,
                    var message : String? = null,
                    var type : Int = 0,
                    var datetime : Long? = null)