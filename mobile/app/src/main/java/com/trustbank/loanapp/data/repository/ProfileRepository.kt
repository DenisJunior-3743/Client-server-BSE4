package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.model.ApplicantProfile

interface ProfileRepository {
    suspend fun getProfile(): ApplicantProfile
    suspend fun updateProfile(profile: ApplicantProfile): ApplicantProfile
}
