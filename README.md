# QuranPlayer
A media player for The Holy Quran


## System Requirements

FR#  | Functional Requirements
-----|----------------------------------------------------------------------
FR1. | QP shall use voice recognition to play a recitation of Quran in an exclusively hands free mode to remain compliant with Android Auto and general safety considerations.
FR2. | QP has an offline mode where it will use efficient algorithms to search the Quran collection in local storage after the user's voice input.
FR3. | QP has an online mode where it will use efficient algorithms to search the Quran collection in a server after the user's voice input.
FR4. | QP will handle a voice input for "pausing" by finishing the verse that it is currently playing then pausing (out of respect).

NFR#  | Non Functional Requirements
------|------------------------------
NFR1. | QP shall offer an advanced setting for expanding choices on qira'a (variant recitations of the Quran that are accepted).
NFR2. | QP will offer "varying speed" of recitation by selecting an accepted recitation from a reciter with faster/slower speed (more respectful than speeding up an existing recitation)
NFR3. | QP will automatically turn off recitation (after completing the verse) after X time specified by user (intended for users who are doing an activity other than driving because they may want to keep track of time consumed during activity). 

#### System Decomposition
---
![QPSystems.drawio.png](..%2F..%2FDownloads%2FQPSystems.drawio.png)

## Abstract and High Level Use Cases

|UC1. User plays a session of offline mode for driving         |
|--------------------------------------------------------------|
|TUCBW: User gives a voice command for first verse of AlFatihah|
|TUCEW: User gives a voice command to pause                    |

#### Use Case Expanded
|UC1. User plays a session of offline mode for driving|
|-----------------------------------------------------|
|Preconditions: User has app running in car           |

|Actor: User                                                                | System: QP                                                                                               |
|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
|                                                                           |0 System is in a listening state for a user voice event in offline mode                                   |    
|1 TUCBW: User gives a voice command for reciting AlFatihah, start verse 1  |2.1 System retrieves the audio files starting from Al Fatihah using efficient search algorithms           |
|                                                                           |2.2 System plays audio files in sequential order and assumes playing the next chapter without interruption|
|3 TUCEW: User gives a voice command for pausing the recitation             |                                                                                                          |
___________________________________________________________________________________________________________________
*Postconditions: The system stores a "load" for the user of where in the recitation he stopped for resuming later*
___________________________________________________________________________________________________________________

