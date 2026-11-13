package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.ApplicantProfile
import kotlinx.coroutines.delay

interface ProfileRepository {
    suspend fun getProfile(): ApplicantProfile
    suspend fun updateProfile(profile: ApplicantProfile): ApplicantProfile
}

class FakeProfileRepository : ProfileRepository {
    override suspend fun getProfile(): ApplicantProfile {
        delay(200)
        return MockData.applicantProfile
    }

    override suspend fun updateProfile(profile: ApplicantProfile): ApplicantProfile {
        delay(400)
        MockData.applicantProfile = profile
        return profile
    }
}
